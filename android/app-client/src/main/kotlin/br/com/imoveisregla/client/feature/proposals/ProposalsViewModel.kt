package br.com.imoveisregla.client.feature.proposals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.imoveisregla.core.data.ApplicationRepository
import br.com.imoveisregla.core.data.AuthRepository
import br.com.imoveisregla.core.data.SessionState
import br.com.imoveisregla.core.model.Application
import br.com.imoveisregla.core.model.ApplicationStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

enum class TimelineState { DONE, CURRENT, WARNING, FAILED, PENDING }

data class TimelineStep(val label: String, val state: TimelineState)

/** Horizontal status timeline: Enviada → Negociação → Documentos → Aprovada (or Recusada / Cancelada). */
fun proposalTimeline(status: ApplicationStatus): List<TimelineStep> {
    val d = TimelineState.DONE
    val p = TimelineState.PENDING
    return when (status) {
        ApplicationStatus.SUBMITTED, ApplicationStatus.UNDER_REVIEW -> listOf(
            TimelineStep("Enviada", TimelineState.CURRENT), TimelineStep("Negociação", p),
            TimelineStep("Documentos", p), TimelineStep("Aprovada", p),
        )
        ApplicationStatus.NEGOTIATING -> listOf(
            TimelineStep("Enviada", d), TimelineStep("Negociação", TimelineState.CURRENT),
            TimelineStep("Documentos", p), TimelineStep("Aprovada", p),
        )
        ApplicationStatus.ACCEPTED, ApplicationStatus.DOCS_REQUESTED -> listOf(
            TimelineStep("Enviada", d), TimelineStep("Negociação", d),
            TimelineStep("Documentos", TimelineState.WARNING), TimelineStep("Aprovada", p),
        )
        ApplicationStatus.DOCS_REVIEW -> listOf(
            TimelineStep("Enviada", d), TimelineStep("Negociação", d),
            TimelineStep("Documentos", TimelineState.CURRENT), TimelineStep("Aprovada", p),
        )
        ApplicationStatus.APPROVED -> listOf(
            TimelineStep("Enviada", d), TimelineStep("Negociação", d),
            TimelineStep("Documentos", d), TimelineStep("Aprovada", d),
        )
        ApplicationStatus.REJECTED -> listOf(TimelineStep("Enviada", d), TimelineStep("Recusada", TimelineState.FAILED))
        ApplicationStatus.WITHDRAWN -> listOf(TimelineStep("Enviada", d), TimelineStep("Cancelada", TimelineState.FAILED))
    }
}

private val BR_DATE: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

/** ISO timestamp/date → "04/10/2026" (local BRT date); empty / unparseable → "". */
fun formatProposalDate(iso: String): String {
    if (iso.isBlank()) return ""
    runCatching {
        return OffsetDateTime.parse(iso).atZoneSameInstant(ZoneOffset.ofHours(-3)).toLocalDate().format(BR_DATE)
    }
    return runCatching { LocalDate.parse(iso.take(10)).format(BR_DATE) }.getOrDefault("")
}

data class ProposalsUiState(
    val signedIn: Boolean = true,
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val error: String? = null,
    val items: List<Application> = emptyList(),
    val confirmWithdraw: Application? = null,
    val withdrawingId: Long? = null,
    val message: String? = null,
)

class ProposalsViewModel(
    private val auth: AuthRepository,
    private val applications: ApplicationRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ProposalsUiState())
    val state: StateFlow<ProposalsUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            auth.session.collect { s ->
                when (s) {
                    is SessionState.SignedIn -> {
                        _state.update { it.copy(signedIn = true) }
                        load(refresh = false)
                    }
                    SessionState.SignedOut -> _state.value = ProposalsUiState(signedIn = false, loading = false)
                    SessionState.Loading -> _state.update { it.copy(loading = true) }
                }
            }
        }
    }

    fun refresh() = load(refresh = true)

    fun retry() = load(refresh = false)

    private fun load(refresh: Boolean) {
        _state.update { if (refresh) it.copy(refreshing = true) else it.copy(loading = it.items.isEmpty(), error = null) }
        viewModelScope.launch {
            try {
                val items = applications.mine()
                _state.update { it.copy(loading = false, refreshing = false, error = null, items = items) }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        loading = false, refreshing = false,
                        error = if (it.items.isEmpty()) e.message ?: "Não foi possível carregar suas propostas" else null,
                        message = if (it.items.isNotEmpty()) e.message ?: "Falha ao atualizar" else null,
                    )
                }
            }
        }
    }

    /** Opens the confirm dialog — only for open proposals. */
    fun askWithdraw(app: Application) {
        if (!app.status.isOpen) return
        _state.update { it.copy(confirmWithdraw = app) }
    }

    fun dismissWithdraw() {
        _state.update { it.copy(confirmWithdraw = null) }
    }

    fun confirmWithdraw() {
        val app = _state.value.confirmWithdraw ?: return
        _state.update { it.copy(confirmWithdraw = null) }
        withdraw(app)
    }

    /** Cancels [app]; ignored for proposals that are no longer open. */
    fun withdraw(app: Application) {
        if (!app.status.isOpen || _state.value.withdrawingId != null) return
        _state.update { it.copy(withdrawingId = app.id) }
        viewModelScope.launch {
            try {
                applications.withdraw(app.id)
                val items = applications.mine()
                _state.update { it.copy(withdrawingId = null, items = items, message = "Proposta cancelada") }
            } catch (e: Exception) {
                _state.update { it.copy(withdrawingId = null, message = e.message ?: "Não foi possível cancelar") }
            }
        }
    }

    fun messageShown() {
        _state.update { it.copy(message = null) }
    }
}
