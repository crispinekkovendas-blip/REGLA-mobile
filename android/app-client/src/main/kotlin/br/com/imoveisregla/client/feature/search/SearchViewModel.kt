package br.com.imoveisregla.client.feature.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.imoveisregla.core.data.AppContainer
import br.com.imoveisregla.core.data.NotAuthenticatedException
import br.com.imoveisregla.core.data.SessionState
import br.com.imoveisregla.core.model.Listing
import br.com.imoveisregla.core.model.ListingFilters
import br.com.imoveisregla.core.model.ListingType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

const val SEARCH_DEBOUNCE_MS = 300L
const val FAVORITE_LOGIN_MESSAGE = "Entre para salvar favoritos"

/** Max-price options offered by the "Preço máx" chip (null = sem limite). */
val PRICE_OPTIONS: List<Long?> = listOf(3_000L, 5_000L, 10_000L, null)
val BED_OPTIONS: List<Int?> = listOf(1, 2, 3, null)

data class SearchUiState(
    /** Text currently in the search field (applied to [filters] after the debounce). */
    val query: String = "",
    val filters: ListingFilters = ListingFilters(),
    val listings: List<Listing> = emptyList(),
    /** Every city seen in results so far (stable chip options even when a city is selected). */
    val cities: List<String> = emptyList(),
    val favoriteIds: Set<Long> = emptySet(),
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val error: String? = null,
    /** One-shot snackbar message; call [SearchViewModel.messageShown] after displaying it. */
    val message: String? = null,
)

class SearchViewModel(private val container: AppContainer) : ViewModel() {

    private val _state = MutableStateFlow(SearchUiState())
    val state: StateFlow<SearchUiState> = _state.asStateFlow()

    private var loadJob: Job? = null
    private var queryJob: Job? = null

    init {
        load()
        // Reload favorites whenever the user signs in / out.
        viewModelScope.launch {
            container.auth.session
                .map { it is SessionState.SignedIn }
                .distinctUntilChanged()
                .drop(1)
                .collect { refreshFavorites() }
        }
    }

    fun setQuery(query: String) {
        _state.update { it.copy(query = query) }
        queryJob?.cancel()
        queryJob = viewModelScope.launch {
            delay(SEARCH_DEBOUNCE_MS)
            applyFilters(_state.value.filters.copy(query = query.trim()))
        }
    }

    fun setType(type: ListingType?) = applyFilters(_state.value.filters.copy(type = type))
    fun setMinBeds(minBeds: Int?) = applyFilters(_state.value.filters.copy(minBeds = minBeds))
    fun setMaxPrice(maxPrice: Long?) = applyFilters(_state.value.filters.copy(maxPrice = maxPrice))
    fun setCity(city: String?) = applyFilters(_state.value.filters.copy(city = city))

    fun clearFilters() {
        queryJob?.cancel()
        _state.update { it.copy(query = "") }
        applyFilters(ListingFilters())
    }

    private fun applyFilters(filters: ListingFilters) {
        if (filters == _state.value.filters && !_state.value.loading && _state.value.error == null) return
        _state.update { it.copy(filters = filters) }
        load()
    }

    fun retry() = load()

    fun refresh() = load(pullToRefresh = true)

    private fun load(pullToRefresh: Boolean = false) {
        loadJob?.cancel()
        _state.update { it.copy(loading = !pullToRefresh, refreshing = pullToRefresh, error = null) }
        loadJob = viewModelScope.launch {
            try {
                val results = container.listings.search(_state.value.filters)
                val favs = fetchFavoriteIds()
                _state.update { s ->
                    s.copy(
                        listings = results,
                        cities = (s.cities + results.map { it.city }).filter { it.isNotBlank() }.distinct().sorted(),
                        favoriteIds = favs ?: s.favoriteIds,
                        loading = false,
                        refreshing = false,
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update {
                    it.copy(loading = false, refreshing = false, error = e.message ?: "Não foi possível carregar os imóveis")
                }
            }
        }
    }

    /** Re-reads favorite ids (e.g. when returning from the detail screen). */
    fun refreshFavorites() {
        viewModelScope.launch {
            val favs = fetchFavoriteIds() ?: return@launch
            _state.update { it.copy(favoriteIds = favs) }
        }
    }

    /** Null when the ids could not be read for a reason other than being signed out. */
    private suspend fun fetchFavoriteIds(): Set<Long>? =
        if (container.auth.session.value !is SessionState.SignedIn) emptySet()
        else try {
            container.favorites.ids()
        } catch (e: CancellationException) {
            throw e
        } catch (e: NotAuthenticatedException) {
            emptySet()
        } catch (e: Exception) {
            null
        }

    fun toggleFavorite(listingId: Long) {
        if (container.auth.session.value !is SessionState.SignedIn) {
            _state.update { it.copy(message = FAVORITE_LOGIN_MESSAGE) }
            return
        }
        val wasFavorite = listingId in _state.value.favoriteIds
        _state.update { it.copy(favoriteIds = if (wasFavorite) it.favoriteIds - listingId else it.favoriteIds + listingId) }
        viewModelScope.launch {
            try {
                container.favorites.set(listingId, !wasFavorite)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val msg = if (e is NotAuthenticatedException) FAVORITE_LOGIN_MESSAGE
                else e.message ?: "Não foi possível atualizar o favorito"
                _state.update {
                    it.copy(
                        favoriteIds = if (wasFavorite) it.favoriteIds + listingId else it.favoriteIds - listingId,
                        message = msg,
                    )
                }
            }
        }
    }

    fun messageShown() = _state.update { it.copy(message = null) }
}
