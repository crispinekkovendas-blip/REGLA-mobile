package br.com.imoveisregla.client.feature.visits

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.imoveisregla.client.feature.visit.SAO_PAULO
import br.com.imoveisregla.client.feature.visit.parseStart
import br.com.imoveisregla.core.data.AppContainer
import br.com.imoveisregla.core.data.SessionState
import br.com.imoveisregla.core.model.Showing
import br.com.imoveisregla.core.model.ShowingStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.ZonedDateTime

data class VisitsUiState(
    val loading: Boolean = true,
    val signedIn: Boolean = true,
    val refreshing: Boolean = false,
    val error: String? = null,
    val upcoming: List<Showing> = emptyList(),
    val past: List<Showing> = emptyList(),
    val cancellingId: Long? = null,
    val message: String? = null,
)

/** Upcoming = starts in the future and not cancelled (soonest first); past = the rest (latest first). */
fun splitVisits(all: List<Showing>, now: ZonedDateTime): Pair<List<Showing>, List<Showing>> {
    val nowI = now.toInstant()
    fun start(s: Showing) = parseStart(s.startsAt)?.toInstant()
    val (up, past) = all.partition { s ->
        val t = start(s)
        s.status != ShowingStatus.CANCELLED && t != null && !t.isBefore(nowI)
    }
    return up.sortedBy { start(it) } to past.sortedByDescending { start(it) }
}

fun canCancel(s: Showing, now: ZonedDateTime): Boolean {
    if (s.status != ShowingStatus.SCHEDULED && s.status != ShowingStatus.CONFIRMED) return false
    val t = parseStart(s.startsAt) ?: return false
    return t.isAfter(now)
}

class VisitsViewModel(
    private val container: AppContainer,
    private val clock: () -> ZonedDateTime = { ZonedDateTime.now(SAO_PAULO) },
) : ViewModel() {

    private val _state = MutableStateFlow(VisitsUiState())
    val state: StateFlow<VisitsUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            container.auth.session.collect { s ->
                when (s) {
                    SessionState.Loading -> _state.update { it.copy(loading = true) }
                    SessionState.SignedOut -> _state.value = VisitsUiState(loading = false, signedIn = false)
                    is SessionState.SignedIn -> {
                        _state.update { it.copy(signedIn = true, loading = true) }
                        load()
                    }
                }
            }
        }
    }

    fun now(): ZonedDateTime = clock()

    fun refresh() {
        if (!_state.value.signedIn) return
        _state.update { it.copy(refreshing = true) }
        viewModelScope.launch { load() }
    }

    fun retry() {
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch { load() }
    }

    private suspend fun load() {
        try {
            val (up, past) = splitVisits(container.visits.mine(), clock())
            _state.update { it.copy(loading = false, refreshing = false, error = null, upcoming = up, past = past) }
        } catch (e: Exception) {
            _state.update {
                it.copy(loading = false, refreshing = false, error = e.message ?: "Não foi possível carregar suas visitas")
            }
        }
    }

    fun cancel(id: Long) {
        if (_state.value.cancellingId != null) return
        _state.update { it.copy(cancellingId = id) }
        viewModelScope.launch {
            try {
                container.visits.cancel(id)
                load()
                _state.update { it.copy(cancellingId = null, message = "Visita cancelada") }
            } catch (e: Exception) {
                _state.update { it.copy(cancellingId = null, message = e.message ?: "Não foi possível cancelar a visita") }
            }
        }
    }

    fun messageShown() = _state.update { it.copy(message = null) }
}
