package br.com.imoveisregla.client.feature.listing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.imoveisregla.core.data.AppContainer
import br.com.imoveisregla.core.data.NotAuthenticatedException
import br.com.imoveisregla.core.data.SessionState
import br.com.imoveisregla.core.model.Listing
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface ListingDetailUiState {
    data object Loading : ListingDetailUiState
    data object NotFound : ListingDetailUiState
    data class Error(val message: String) : ListingDetailUiState
    data class Loaded(
        val listing: Listing,
        val photoUrls: List<String>,
        val isFavorite: Boolean = false,
        val message: String? = null,
    ) : ListingDetailUiState
}

class ListingDetailViewModel(
    private val container: AppContainer,
    private val listingId: Long,
) : ViewModel() {

    private val _state = MutableStateFlow<ListingDetailUiState>(ListingDetailUiState.Loading)
    val state: StateFlow<ListingDetailUiState> = _state.asStateFlow()

    init { load() }

    val isSignedIn: Boolean get() = container.auth.session.value is SessionState.SignedIn

    fun load() {
        _state.value = ListingDetailUiState.Loading
        viewModelScope.launch {
            _state.value = try {
                val listing = container.listings.get(listingId)
                val photos = listing.photos.sortedBy { it.position }.map { container.listings.photoUrl(it.storagePath) }
                ListingDetailUiState.Loaded(listing, photos, isFavorite = favoriteIds()?.contains(listingId) == true)
            } catch (e: CancellationException) {
                throw e
            } catch (e: NoSuchElementException) {
                ListingDetailUiState.NotFound
            } catch (e: Exception) {
                ListingDetailUiState.Error(e.message ?: "Não foi possível carregar o imóvel")
            }
        }
    }

    /** Re-reads the favorite flag (e.g. after signing in on the login screen). */
    fun refreshFavorite() {
        if (_state.value !is ListingDetailUiState.Loaded) return
        viewModelScope.launch {
            val ids = favoriteIds() ?: return@launch
            _state.update { s -> (s as? ListingDetailUiState.Loaded)?.copy(isFavorite = listingId in ids) ?: s }
        }
    }

    private suspend fun favoriteIds(): Set<Long>? =
        if (!isSignedIn) emptySet()
        else try {
            container.favorites.ids()
        } catch (e: CancellationException) {
            throw e
        } catch (e: NotAuthenticatedException) {
            emptySet()
        } catch (e: Exception) {
            null
        }

    fun toggleFavorite() {
        val loaded = _state.value as? ListingDetailUiState.Loaded ?: return
        val target = !loaded.isFavorite
        _state.value = loaded.copy(isFavorite = target)
        viewModelScope.launch {
            try {
                container.favorites.set(listingId, target)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val msg = if (e is NotAuthenticatedException) "Entre para salvar favoritos"
                else e.message ?: "Não foi possível atualizar o favorito"
                _state.update { s -> (s as? ListingDetailUiState.Loaded)?.copy(isFavorite = !target, message = msg) ?: s }
            }
        }
    }

    fun messageShown() {
        _state.update { s -> (s as? ListingDetailUiState.Loaded)?.copy(message = null) ?: s }
    }
}
