package br.com.imoveisregla.realtor.feature.listings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.imoveisregla.core.data.ListingRepository
import br.com.imoveisregla.core.model.Listing
import br.com.imoveisregla.core.model.ListingStatus
import br.com.imoveisregla.core.model.label
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ListingsUiState(
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val error: String? = null,
    val all: List<Listing> = emptyList(),
    val status: ListingStatus? = null,
    val query: String = "",
    val message: String? = null,
) {
    val counts: Map<ListingStatus, Int> get() = statusCounts(all)
    val visible: List<Listing> get() = filterListings(all, status, query)
}

class ListingsViewModel(private val listings: ListingRepository) : ViewModel() {
    private val _state = MutableStateFlow(ListingsUiState())
    val state: StateFlow<ListingsUiState> = _state.asStateFlow()

    init { load() }

    fun load(refresh: Boolean = false) {
        _state.update { it.copy(loading = !refresh && it.all.isEmpty(), refreshing = refresh, error = null) }
        viewModelScope.launch {
            try {
                val rows = listings.all()
                _state.update { it.copy(all = rows, loading = false, refreshing = false) }
            } catch (e: Exception) {
                _state.update {
                    it.copy(loading = false, refreshing = false, error = e.message ?: "Não foi possível carregar os imóveis")
                }
            }
        }
    }

    fun refresh() = load(refresh = true)
    fun setStatusFilter(status: ListingStatus?) = _state.update { it.copy(status = status) }
    fun setQuery(query: String) = _state.update { it.copy(query = query) }

    fun setStatus(id: Long, status: ListingStatus) {
        viewModelScope.launch {
            try {
                listings.setStatus(id, status)
                val rows = listings.all()
                _state.update { it.copy(all = rows, message = "Status alterado para ${status.label}") }
            } catch (e: Exception) {
                _state.update { it.copy(message = e.message ?: "Não foi possível alterar o status") }
            }
        }
    }

    fun messageShown() = _state.update { it.copy(message = null) }
}

data class ListingDetailUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val listing: Listing? = null,
    val saving: Boolean = false,
    val message: String? = null,
)

class ListingDetailViewModel(
    private val listingId: Long,
    private val listings: ListingRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(ListingDetailUiState())
    val state: StateFlow<ListingDetailUiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            try {
                val l = listings.get(listingId)
                _state.update { it.copy(loading = false, listing = l) }
            } catch (e: Exception) {
                _state.update { it.copy(loading = false, error = e.message ?: "Imóvel não encontrado") }
            }
        }
    }

    fun setStatus(status: ListingStatus) {
        if (_state.value.listing?.status == status || _state.value.saving) return
        _state.update { it.copy(saving = true) }
        viewModelScope.launch {
            try {
                listings.setStatus(listingId, status)
                val l = listings.get(listingId)
                _state.update { it.copy(saving = false, listing = l, message = "Status alterado para ${status.label}") }
            } catch (e: Exception) {
                _state.update { it.copy(saving = false, message = e.message ?: "Não foi possível alterar o status") }
            }
        }
    }

    fun photoUrl(path: String): String = listings.photoUrl(path)
    fun showMessage(text: String) = _state.update { it.copy(message = text) }
    fun messageShown() = _state.update { it.copy(message = null) }
}
