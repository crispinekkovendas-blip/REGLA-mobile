package br.com.imoveisregla.client.feature.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.imoveisregla.core.data.AppContainer
import br.com.imoveisregla.core.data.NotAuthenticatedException
import br.com.imoveisregla.core.data.SessionState
import br.com.imoveisregla.core.model.Listing
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface FavoritesUiState {
    data object Loading : FavoritesUiState
    data object SignedOut : FavoritesUiState
    data class Error(val message: String) : FavoritesUiState
    data class Loaded(val listings: List<Listing>, val message: String? = null) : FavoritesUiState
}

class FavoritesViewModel(private val container: AppContainer) : ViewModel() {

    private val _state = MutableStateFlow<FavoritesUiState>(FavoritesUiState.Loading)
    val state: StateFlow<FavoritesUiState> = _state.asStateFlow()

    private var loadJob: Job? = null

    init {
        viewModelScope.launch {
            container.auth.session
                .distinctUntilChanged { a, b -> a::class == b::class }
                .collect { session ->
                    when (session) {
                        SessionState.Loading -> _state.value = FavoritesUiState.Loading
                        SessionState.SignedOut -> {
                            loadJob?.cancel()
                            _state.value = FavoritesUiState.SignedOut
                        }
                        is SessionState.SignedIn -> load()
                    }
                }
        }
    }

    /** Reload (no-op while signed out). Keeps showing the current list while refreshing. */
    fun load() {
        if (container.auth.session.value !is SessionState.SignedIn) {
            _state.value = FavoritesUiState.SignedOut
            return
        }
        loadJob?.cancel()
        if (_state.value !is FavoritesUiState.Loaded) _state.value = FavoritesUiState.Loading
        loadJob = viewModelScope.launch {
            _state.value = try {
                val ids = container.favorites.ids()
                val listings = if (ids.isEmpty()) emptyList() else container.listings.byIds(ids.toList())
                FavoritesUiState.Loaded(listings.filter { it.id in ids })
            } catch (e: CancellationException) {
                throw e
            } catch (e: NotAuthenticatedException) {
                FavoritesUiState.SignedOut
            } catch (e: Exception) {
                FavoritesUiState.Error(e.message ?: "Não foi possível carregar seus favoritos")
            }
        }
    }

    fun remove(listingId: Long) {
        val loaded = _state.value as? FavoritesUiState.Loaded ?: return
        val removed = loaded.listings.firstOrNull { it.id == listingId } ?: return
        val index = loaded.listings.indexOf(removed)
        _state.value = loaded.copy(listings = loaded.listings - removed)
        viewModelScope.launch {
            try {
                container.favorites.set(listingId, false)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { s ->
                    if (s !is FavoritesUiState.Loaded) s
                    else s.copy(
                        listings = s.listings.toMutableList().apply { add(index.coerceAtMost(size), removed) },
                        message = e.message ?: "Não foi possível remover o favorito",
                    )
                }
            }
        }
    }

    fun messageShown() {
        _state.update { s -> (s as? FavoritesUiState.Loaded)?.copy(message = null) ?: s }
    }
}
