package br.com.imoveisregla.realtor.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.imoveisregla.core.data.AppContainer
import br.com.imoveisregla.core.data.SessionState
import br.com.imoveisregla.core.model.Application
import br.com.imoveisregla.core.model.DashboardStats
import br.com.imoveisregla.core.model.Inquiry
import br.com.imoveisregla.core.model.InquiryStage
import br.com.imoveisregla.core.model.Showing
import br.com.imoveisregla.core.model.ShowingStatus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

data class DashboardUiState(
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val stats: DashboardStats? = null,
    val upcomingVisits: List<Showing> = emptyList(),
    val recentProposals: List<Application> = emptyList(),
    val newLeads: List<Inquiry> = emptyList(),
    val error: String? = null,
    val email: String? = null,
)

class DashboardViewModel(
    private val container: AppContainer,
    private val clock: () -> OffsetDateTime = { OffsetDateTime.now(BRT) },
) : ViewModel() {

    private val _state = MutableStateFlow(DashboardUiState())
    val state: StateFlow<DashboardUiState> = _state.asStateFlow()

    val isLive: Boolean get() = container.isLive

    private var loadJob: Job? = null

    init {
        _state.update { it.copy(email = (container.auth.session.value as? SessionState.SignedIn)?.session?.email) }
        // Subscribe first so no realtime event is missed while the first load runs.
        viewModelScope.launch {
            container.dashboard.changes()
                .catch { /* realtime unavailable: pull-to-refresh still works */ }
                .collect { load(silent = true) }
        }
        load()
    }

    /** Pull-to-refresh. */
    fun refresh() = load(userInitiated = true)

    fun signOut() {
        viewModelScope.launch {
            try {
                container.auth.signOut()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message ?: "Não foi possível sair") }
            }
        }
    }

    private fun load(userInitiated: Boolean = false, silent: Boolean = false) {
        loadJob?.cancel()
        _state.update {
            it.copy(
                loading = it.stats == null && !silent,
                refreshing = userInitiated,
                error = if (silent) it.error else null,
            )
        }
        loadJob = viewModelScope.launch {
            try {
                val now = clock()
                val stats = container.dashboard.stats()
                val visits = container.agenda.range(now.toString(), now.plusDays(7).toString())
                    .filter { it.status != ShowingStatus.CANCELLED }
                    .take(3)
                val proposals = container.applications.forReview().take(3)
                val leads = container.leads.list(InquiryStage.INBOX).take(3)
                _state.update {
                    it.copy(
                        loading = false, refreshing = false, stats = stats, upcomingVisits = visits,
                        recentProposals = proposals, newLeads = leads, error = null,
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update {
                    it.copy(loading = false, refreshing = false, error = e.message ?: "Não foi possível carregar o painel")
                }
            }
        }
    }

    companion object {
        val BRT: ZoneId = ZoneId.of("America/Sao_Paulo")
    }
}

private val PT_BR: Locale = Locale.forLanguageTag("pt-BR")

/** "Bom dia" before noon, "Boa tarde" until 18h, "Boa noite" otherwise. */
fun greetingFor(hour: Int): String = when (hour) {
    in 5..11 -> "Bom dia"
    in 12..17 -> "Boa tarde"
    else -> "Boa noite"
}

/** "Sábado, 4 de outubro" */
fun dashboardDate(t: OffsetDateTime): String =
    t.format(DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", PT_BR)).replaceFirstChar { it.titlecase(PT_BR) }

/** "Hoje · 15:00", "Amanhã · 10:00" or "seg, 06/10 · 11:00". */
fun visitWhen(startsAtIso: String, now: OffsetDateTime): String {
    val t = runCatching { OffsetDateTime.parse(startsAtIso).atZoneSameInstant(now.offset).toOffsetDateTime() }
        .getOrNull() ?: return startsAtIso
    val time = t.format(DateTimeFormatter.ofPattern("HH:mm", PT_BR))
    val day = when (t.toLocalDate()) {
        now.toLocalDate() -> "Hoje"
        now.toLocalDate().plusDays(1) -> "Amanhã"
        else -> t.format(DateTimeFormatter.ofPattern("EEE, dd/MM", PT_BR)).replace(".", "")
    }
    return "$day · $time"
}
