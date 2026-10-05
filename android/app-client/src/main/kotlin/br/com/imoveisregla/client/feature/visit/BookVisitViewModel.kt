package br.com.imoveisregla.client.feature.visit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.imoveisregla.core.data.AppContainer
import br.com.imoveisregla.core.data.SessionState
import br.com.imoveisregla.core.model.Listing
import br.com.imoveisregla.core.model.UserSession
import br.com.imoveisregla.core.model.VisitInput
import br.com.imoveisregla.core.model.digitsOnly
import br.com.imoveisregla.core.model.maskPhoneBR
import br.com.imoveisregla.core.model.validateVisit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZonedDateTime

data class BookVisitUiState(
    val loading: Boolean = true,
    val signedIn: Boolean = true,
    val listing: Listing? = null,
    val photoUrl: String? = null,
    val loadError: String? = null,
    val days: List<LocalDate> = emptyList(),
    val selectedDay: LocalDate? = null,
    val slots: List<Slot> = emptyList(),
    val selectedSlot: String? = null,
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val notes: String = "",
    val errors: Map<String, String> = emptyMap(),
    val submitting: Boolean = false,
    val submitError: String? = null,
    val done: Boolean = false,
)

class BookVisitViewModel(
    private val listingId: Long,
    private val container: AppContainer,
    private val clock: () -> ZonedDateTime = { ZonedDateTime.now(SAO_PAULO) },
) : ViewModel() {

    private val _state = MutableStateFlow(BookVisitUiState())
    val state: StateFlow<BookVisitUiState> = _state.asStateFlow()

    private var loadedFor: String? = null

    init {
        viewModelScope.launch {
            container.auth.session.collect { s ->
                when (s) {
                    SessionState.Loading -> _state.update { it.copy(loading = true) }
                    SessionState.SignedOut -> {
                        loadedFor = null
                        _state.update { it.copy(loading = false, signedIn = false) }
                    }
                    is SessionState.SignedIn -> if (loadedFor != s.session.userId) {
                        loadedFor = s.session.userId
                        load(s.session)
                    }
                }
            }
        }
    }

    fun retry() {
        val s = container.auth.session.value as? SessionState.SignedIn ?: return
        viewModelScope.launch { load(s.session) }
    }

    private suspend fun load(session: UserSession) {
        _state.update { it.copy(loading = true, signedIn = true, loadError = null) }
        val listing = try {
            container.listings.get(listingId)
        } catch (e: Exception) {
            _state.update { it.copy(loading = false, loadError = e.message ?: "Não foi possível carregar o imóvel") }
            return
        }
        val profile = try { container.profiles.mine() } catch (_: Exception) { null }
        val now = clock()
        val days = nextDays(now.toLocalDate())
        val firstDay = days.firstOrNull { d -> slotsFor(d, now).any { it.enabled } } ?: days.firstOrNull()
        val photo = listing.photos.minByOrNull { it.position }?.let { container.listings.photoUrl(it.storagePath) }
        _state.update { cur ->
            cur.copy(
                loading = false,
                listing = listing,
                photoUrl = photo,
                days = days,
                selectedDay = firstDay,
                slots = firstDay?.let { slotsFor(it, now) }.orEmpty(),
                selectedSlot = null,
                // keep anything the user already typed
                name = cur.name.ifBlank { profile?.fullName.orEmpty() },
                email = cur.email.ifBlank { profile?.email?.takeIf { it.isNotBlank() } ?: session.email },
                phone = cur.phone.ifBlank { maskPhoneBR(profile?.phone.orEmpty()) },
            )
        }
    }

    fun selectDay(day: LocalDate) {
        _state.update {
            it.copy(
                selectedDay = day, slots = slotsFor(day, clock()), selectedSlot = null,
                errors = it.errors - "startsAt",
            )
        }
    }

    fun selectSlot(slot: Slot) {
        if (!slot.enabled) return
        _state.update { it.copy(selectedSlot = slot.isoStart, errors = it.errors - "startsAt") }
    }

    fun onName(v: String) = _state.update { it.copy(name = v, errors = it.errors - "visitorName") }
    fun onEmail(v: String) = _state.update { it.copy(email = v, errors = it.errors - "visitorEmail") }
    fun onPhone(v: String) = _state.update { it.copy(phone = maskPhoneBR(v), errors = it.errors - "visitorPhone") }
    fun onNotes(v: String) = _state.update { it.copy(notes = v.take(500)) }

    fun confirm() {
        val s = _state.value
        if (s.submitting || s.done) return
        val input = VisitInput(
            listingId = listingId,
            startsAt = s.selectedSlot.orEmpty(),
            visitorName = s.name.trim(),
            visitorEmail = s.email.trim(),
            visitorPhone = s.phone.takeIf { it.isNotBlank() },
            notes = s.notes.trim().takeIf { it.isNotEmpty() },
        )
        val errors = buildMap {
            putAll(validateVisit(input))
            val digits = digitsOnly(s.phone)
            if (digits.isNotEmpty() && digits.length < 10) put("visitorPhone", "Telefone inválido")
            // the slot may have expired while the screen was open
            val start = s.selectedSlot?.let { parseStart(it) }
            if (start != null && start.isBefore(clock().plusMinutes(60))) put("startsAt", "Este horário não está mais disponível")
        }
        if (errors.isNotEmpty()) {
            _state.update { it.copy(errors = errors, submitError = null) }
            return
        }
        _state.update { it.copy(submitting = true, errors = emptyMap(), submitError = null) }
        viewModelScope.launch {
            try {
                container.visits.book(input)
                _state.update { it.copy(submitting = false, done = true) }
            } catch (e: Exception) {
                _state.update { it.copy(submitting = false, submitError = e.message ?: "Não foi possível agendar a visita") }
            }
        }
    }
}
