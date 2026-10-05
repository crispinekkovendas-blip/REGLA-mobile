package br.com.imoveisregla.realtor.feature.proposals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.imoveisregla.core.data.ApplicationRepository
import br.com.imoveisregla.core.data.DocumentRepository
import br.com.imoveisregla.core.model.Application
import br.com.imoveisregla.core.model.ApplicationStatus
import br.com.imoveisregla.core.model.ClientDocument
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private fun Throwable.userMessage(): String = message?.takeIf { it.isNotBlank() } ?: "Não foi possível carregar"

// ─── List ─────────────────────────────────────────────────────────────

data class ProposalsState(
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val error: String? = null,
    val all: List<Application> = emptyList(),
    val filter: ApplicationStatus? = null,
) {
    val counts: Map<ApplicationStatus?, Int> get() = statusCounts(all)
    val visible: List<Application> get() = filterApplications(all, filter)
}

class ProposalsViewModel(private val applications: ApplicationRepository) : ViewModel() {
    private val _state = MutableStateFlow(ProposalsState())
    val state: StateFlow<ProposalsState> = _state.asStateFlow()

    init { load() }

    fun setFilter(status: ApplicationStatus?) = _state.update { it.copy(filter = status) }

    fun refresh() = load(refreshing = true)

    fun load(refreshing: Boolean = false) {
        _state.update { it.copy(loading = !refreshing && it.all.isEmpty(), refreshing = refreshing, error = null) }
        viewModelScope.launch {
            try {
                // Load everything once so chip counts stay correct; filter locally.
                val rows = applications.forReview(null)
                _state.update { it.copy(loading = false, refreshing = false, all = rows) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(loading = false, refreshing = false, error = e.userMessage()) }
            }
        }
    }
}

// ─── Detail ───────────────────────────────────────────────────────────

data class ApplicationDetailState(
    val loading: Boolean = true,
    val error: String? = null,
    val application: Application? = null,
    val documents: List<ClientDocument> = emptyList(),
    val documentsError: String? = null,
    val submitting: Boolean = false,
    /** One-shot snackbar text. */
    val message: String? = null,
) {
    val canAct: Boolean get() = application?.status?.isOpen == true && !submitting
}

class ApplicationDetailViewModel(
    private val applications: ApplicationRepository,
    private val documents: DocumentRepository,
    private val applicationId: Long,
) : ViewModel() {
    private val _state = MutableStateFlow(ApplicationDetailState())
    val state: StateFlow<ApplicationDetailState> = _state.asStateFlow()

    init { load() }

    fun load() {
        _state.update { it.copy(loading = it.application == null, error = null) }
        viewModelScope.launch { reload() }
    }

    private suspend fun reload() {
        try {
            val app = applications.get(applicationId)
            _state.update { it.copy(loading = false, application = app) }
            try {
                val docs = documents.list(userId = app.userId)
                _state.update { it.copy(documents = docs, documentsError = null) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(documentsError = e.userMessage()) }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            _state.update { it.copy(loading = false, error = e.userMessage()) }
        }
    }

    /**
     * Moves the proposal to [target]. Returns a validation error (also shown as snackbar)
     * or null when the request was dispatched.
     */
    fun review(target: ApplicationStatus, note: String?): String? {
        val current = _state.value.application?.status ?: return "Proposta não carregada"
        if (_state.value.submitting) return "Aguarde…"
        val cleanNote = note?.trim()?.takeIf { it.isNotEmpty() }
        validateReview(current, target, cleanNote)?.let { err ->
            _state.update { it.copy(message = err) }
            return err
        }
        _state.update { it.copy(submitting = true) }
        viewModelScope.launch {
            try {
                applications.review(applicationId, target, cleanNote)
                _state.update { it.copy(submitting = false, message = reviewSuccessMessage(target)) }
                reload()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(submitting = false, message = e.message ?: "Não foi possível atualizar a proposta") }
            }
        }
        return null
    }

    suspend fun documentUrl(doc: ClientDocument): String = documents.signedUrl(doc.storagePath)

    fun showMessage(text: String) = _state.update { it.copy(message = text) }

    fun consumeMessage() = _state.update { it.copy(message = null) }
}
