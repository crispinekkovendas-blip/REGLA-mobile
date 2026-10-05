package br.com.imoveisregla.realtor.feature.leads

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.imoveisregla.core.data.AgendaRepository
import br.com.imoveisregla.core.data.LeadRepository
import br.com.imoveisregla.core.model.Inquiry
import br.com.imoveisregla.core.model.InquiryStage
import br.com.imoveisregla.core.model.LeadNote
import br.com.imoveisregla.core.model.Priority
import br.com.imoveisregla.core.model.label
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.OffsetDateTime

data class LeadDetailUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val lead: Inquiry? = null,
    val notes: List<LeadNote> = emptyList(),
    val phone: String? = null,
    val noteDraft: String = "",
    val sendingNote: Boolean = false,
    val saving: Boolean = false,
    val message: String? = null,
)

class LeadDetailViewModel(
    private val leadId: Long,
    private val leads: LeadRepository,
    private val agenda: AgendaRepository,
    private val now: () -> OffsetDateTime = { OffsetDateTime.now() },
) : ViewModel() {
    private val _state = MutableStateFlow(LeadDetailUiState())
    val state: StateFlow<LeadDetailUiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            try {
                var lead = leads.get(leadId)
                if (!lead.read) {
                    leads.update(leadId, read = true)
                    lead = lead.copy(read = true)
                }
                val notes = leads.notes(leadId)
                _state.update { it.copy(loading = false, lead = lead, notes = notes) }
                lookupPhone(lead.email)
            } catch (e: Exception) {
                _state.update { it.copy(loading = false, error = e.message ?: "Não foi possível carregar o lead") }
            }
        }
    }

    private suspend fun lookupPhone(email: String) {
        val t = now()
        val phone = try {
            phoneFromShowings(email, agenda.range(t.minusDays(180).toString(), t.plusDays(180).toString()))
        } catch (e: Exception) {
            null
        }
        _state.update { it.copy(phone = phone) }
    }

    fun setStage(stage: InquiryStage) {
        if (_state.value.lead?.stage == stage) return
        save("Etapa alterada para ${stage.label}") { leads.update(leadId, stage = stage) }
    }

    fun setPriority(priority: Priority) {
        if (_state.value.lead?.priority == priority) return
        save("Prioridade: ${priority.label}") { leads.update(leadId, priority = priority) }
    }

    private fun save(success: String, block: suspend () -> Unit) {
        _state.update { it.copy(saving = true) }
        viewModelScope.launch {
            try {
                block()
                val lead = leads.get(leadId)
                _state.update { it.copy(saving = false, lead = lead, message = success) }
            } catch (e: Exception) {
                _state.update { it.copy(saving = false, message = e.message ?: "Não foi possível salvar") }
            }
        }
    }

    fun setNoteDraft(text: String) = _state.update { it.copy(noteDraft = text) }

    fun addNote() {
        val body = _state.value.noteDraft.trim()
        if (body.isEmpty() || _state.value.sendingNote) return
        _state.update { it.copy(sendingNote = true) }
        viewModelScope.launch {
            try {
                leads.addNote(leadId, body)
                val notes = leads.notes(leadId)
                _state.update { it.copy(sendingNote = false, notes = notes, noteDraft = "", message = "Nota adicionada") }
            } catch (e: Exception) {
                _state.update { it.copy(sendingNote = false, message = e.message ?: "Não foi possível salvar a nota") }
            }
        }
    }

    fun showMessage(text: String) = _state.update { it.copy(message = text) }
    fun messageShown() = _state.update { it.copy(message = null) }
}
