package br.com.imoveisregla.realtor.feature.agenda

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.imoveisregla.core.data.AgendaRepository
import br.com.imoveisregla.core.model.Showing
import br.com.imoveisregla.core.model.ShowingStatus
import br.com.imoveisregla.core.model.label
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

data class AgendaState(
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val error: String? = null,
    val today: LocalDate,
    val selected: LocalDate,
    val byDay: Map<LocalDate, List<Showing>> = emptyMap(),
    /** Showing ids with a status update in flight. */
    val busy: Set<Long> = emptySet(),
    /** One-shot snackbar text. */
    val message: String? = null,
) {
    val days: List<LocalDate> get() = agendaDays(today)
    val selectedShowings: List<Showing> get() = byDay[selected].orEmpty()
    val all: List<Showing> get() = byDay.values.flatten()
}

class AgendaViewModel(
    private val agenda: AgendaRepository,
    private val zone: ZoneId = SAO_PAULO,
    private val today: () -> LocalDate = { LocalDate.now(zone) },
) : ViewModel() {
    private val _state = MutableStateFlow(today().let { AgendaState(today = it, selected = it) })
    val state: StateFlow<AgendaState> = _state.asStateFlow()

    init { load() }

    fun select(day: LocalDate) = _state.update { it.copy(selected = day) }

    fun refresh() = load(refreshing = true)

    fun load(refreshing: Boolean = false) {
        val d = today()
        _state.update {
            it.copy(
                today = d,
                selected = if (it.selected < d || it.selected >= d.plusDays(AGENDA_DAYS.toLong())) d else it.selected,
                loading = !refreshing && it.byDay.isEmpty(),
                refreshing = refreshing,
                error = null,
            )
        }
        viewModelScope.launch { fetch() }
    }

    private suspend fun fetch() {
        val (from, to) = agendaWindow(_state.value.today, zone)
        try {
            val rows = agenda.range(from, to)
            _state.update { it.copy(loading = false, refreshing = false, byDay = groupByDay(rows, zone)) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            _state.update {
                it.copy(loading = false, refreshing = false, error = e.message ?: "Não foi possível carregar a agenda")
            }
        }
    }

    /** Applies a status change if allowed from the showing's current status. Returns an error or null. */
    fun setStatus(id: Long, target: ShowingStatus): String? {
        val showing = _state.value.all.firstOrNull { it.id == id }
        val err = when {
            showing == null -> "Visita não encontrada"
            id in _state.value.busy -> "Aguarde…"
            !canTransition(showing.status, target) -> "Não é possível mudar de \"${showing.status.label}\" para \"${target.label}\""
            else -> null
        }
        if (err != null) {
            _state.update { it.copy(message = err) }
            return err
        }
        _state.update { it.copy(busy = it.busy + id) }
        viewModelScope.launch {
            try {
                agenda.setStatus(id, target)
                _state.update { it.copy(busy = it.busy - id, message = actionSuccessMessage(target)) }
                fetch()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(busy = it.busy - id, message = e.message ?: "Não foi possível atualizar a visita") }
            }
        }
        return null
    }

    fun showMessage(text: String) = _state.update { it.copy(message = text) }

    fun consumeMessage() = _state.update { it.copy(message = null) }
}
