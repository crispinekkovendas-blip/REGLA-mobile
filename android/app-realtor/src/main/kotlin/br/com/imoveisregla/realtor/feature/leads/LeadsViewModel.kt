package br.com.imoveisregla.realtor.feature.leads

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.imoveisregla.core.data.LeadRepository
import br.com.imoveisregla.core.model.Inquiry
import br.com.imoveisregla.core.model.InquiryStage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LeadsUiState(
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val error: String? = null,
    val all: List<Inquiry> = emptyList(),
    val stage: InquiryStage? = null,
    val query: String = "",
) {
    val counts: Map<InquiryStage, Int> get() = stageCounts(all)
    val visible: List<Inquiry> get() = filterLeads(all, stage, query)
}

class LeadsViewModel(private val leads: LeadRepository) : ViewModel() {
    private val _state = MutableStateFlow(LeadsUiState())
    val state: StateFlow<LeadsUiState> = _state.asStateFlow()

    init { load() }

    fun load(refresh: Boolean = false) {
        _state.update { it.copy(loading = !refresh && it.all.isEmpty(), refreshing = refresh, error = null) }
        viewModelScope.launch {
            try {
                val rows = leads.list(null)
                _state.update { it.copy(all = rows, loading = false, refreshing = false) }
            } catch (e: Exception) {
                _state.update {
                    it.copy(loading = false, refreshing = false, error = e.message ?: "Não foi possível carregar os leads")
                }
            }
        }
    }

    fun refresh() = load(refresh = true)
    fun setStage(stage: InquiryStage?) = _state.update { it.copy(stage = stage) }
    fun setQuery(query: String) = _state.update { it.copy(query = query) }
}
