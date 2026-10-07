package br.com.imoveisregla.client.feature.proposals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.imoveisregla.client.feature.profile.MAX_DOCUMENT_BYTES
import br.com.imoveisregla.client.feature.profile.missingRequiredKinds
import br.com.imoveisregla.core.data.AppContainer
import br.com.imoveisregla.core.model.Application
import br.com.imoveisregla.core.model.ApplicationStatus
import br.com.imoveisregla.core.model.ClientDocument
import br.com.imoveisregla.core.model.DocumentKind
import br.com.imoveisregla.core.model.Party
import br.com.imoveisregla.core.model.UploadFile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** What the client can do right now on a proposta. */
enum class ClientAction { RESPOND_OFFER, SEND_DOCUMENTS, WAIT, CLOSED }

fun clientAction(app: Application): ClientAction = when {
    !app.status.isOpen -> ClientAction.CLOSED
    app.awaiting != Party.CLIENT -> ClientAction.WAIT
    app.status.isNegotiation -> ClientAction.RESPOND_OFFER
    app.status.isDocumentsPhase -> ClientAction.SEND_DOCUMENTS
    else -> ClientAction.WAIT
}

/** One-line headline for the proposta's current state, from the client's point of view. */
fun clientHeadline(app: Application): String = when (app.status) {
    ApplicationStatus.SUBMITTED, ApplicationStatus.UNDER_REVIEW -> "Aguardando resposta do proprietário"
    ApplicationStatus.NEGOTIATING ->
        if (app.awaiting == Party.CLIENT) "O proprietário fez uma contraproposta" else "Aguardando resposta do proprietário"
    ApplicationStatus.ACCEPTED -> "Proposta aceita! Envie seus documentos"
    ApplicationStatus.DOCS_REVIEW -> "Documentos em análise"
    ApplicationStatus.DOCS_REQUESTED -> "O corretor pediu ajustes nos documentos"
    ApplicationStatus.APPROVED -> "Aprovada! O corretor vai enviar o contrato"
    ApplicationStatus.REJECTED -> "Proposta recusada"
    ApplicationStatus.WITHDRAWN -> "Proposta cancelada"
}

enum class ConfirmKind { ACCEPT, DECLINE, SEND_DOCS }

data class ProposalDetailUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val app: Application? = null,
    /** Documents linked to this proposta. */
    val documents: List<ClientDocument> = emptyList(),
    val busy: Boolean = false,
    val uploadingKind: DocumentKind? = null,
    val confirm: ConfirmKind? = null,
    val counterOpen: Boolean = false,
    val counterDigits: String = "",
    val counterMessage: String = "",
    val counterError: String? = null,
    val message: String? = null,
) {
    val action: ClientAction get() = app?.let(::clientAction) ?: ClientAction.WAIT
    val missingDocuments: List<DocumentKind> get() = missingRequiredKinds(documents)
}

class ProposalDetailViewModel(
    private val applicationId: Long,
    private val container: AppContainer,
) : ViewModel() {

    private val _state = MutableStateFlow(ProposalDetailUiState())
    val state: StateFlow<ProposalDetailUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        _state.update { it.copy(loading = it.app == null, error = null) }
        viewModelScope.launch {
            try {
                val app = container.applications.get(applicationId)
                val docs = runCatching { container.documents.list() }.getOrDefault(emptyList())
                    .filter { it.applicationId == applicationId }
                _state.update { it.copy(loading = false, app = app, documents = docs) }
            } catch (e: Exception) {
                _state.update { it.copy(loading = false, error = e.message ?: "Não foi possível carregar a proposta") }
            }
        }
    }

    // ── negotiation ──
    fun ask(kind: ConfirmKind) {
        _state.update { it.copy(confirm = kind) }
    }

    fun dismissConfirm() {
        _state.update { it.copy(confirm = null) }
    }

    fun confirm() {
        val kind = _state.value.confirm ?: return
        _state.update { it.copy(confirm = null) }
        when (kind) {
            ConfirmKind.ACCEPT -> run("Proposta aceita! Agora envie seus documentos.") { container.applications.accept(applicationId) }
            ConfirmKind.DECLINE -> run("Proposta cancelada") { container.applications.decline(applicationId) }
            ConfirmKind.SEND_DOCS -> sendDocuments()
        }
    }

    fun openCounter() {
        val app = _state.value.app ?: return
        _state.update {
            it.copy(counterOpen = true, counterDigits = app.currentPrice.toString(), counterMessage = "", counterError = null)
        }
    }

    fun closeCounter() {
        _state.update { it.copy(counterOpen = false) }
    }

    fun updateCounter(digits: String? = null, message: String? = null) {
        _state.update {
            it.copy(
                counterDigits = digits?.filter(Char::isDigit)?.trimStart('0')?.take(12) ?: it.counterDigits,
                counterMessage = message?.take(500) ?: it.counterMessage,
                counterError = null,
            )
        }
    }

    fun sendCounter() {
        val s = _state.value
        val price = s.counterDigits.toLongOrNull() ?: 0L
        val current = s.app?.currentPrice
        when {
            price <= 0 -> { _state.update { it.copy(counterError = "Informe um valor") }; return }
            price == current -> { _state.update { it.copy(counterError = "Informe um valor diferente da oferta atual") }; return }
        }
        _state.update { it.copy(counterOpen = false) }
        run("Contraproposta enviada") {
            container.applications.counter(applicationId, price, s.counterMessage.trim().ifBlank { null })
        }
    }

    // ── documents ──
    fun upload(kind: DocumentKind, file: UploadFile) {
        if (file.bytes.size > MAX_DOCUMENT_BYTES) {
            _state.update { it.copy(message = "Arquivo maior que 10 MB") }
            return
        }
        _state.update { it.copy(uploadingKind = kind) }
        viewModelScope.launch {
            try {
                val doc = container.documents.upload(kind, file, applicationId = applicationId)
                _state.update { it.copy(uploadingKind = null, documents = listOf(doc) + it.documents) }
            } catch (e: Exception) {
                _state.update { it.copy(uploadingKind = null, message = e.message ?: "Falha no envio do arquivo") }
            }
        }
    }

    fun deleteDocument(id: Long) {
        viewModelScope.launch {
            try {
                container.documents.delete(id)
                _state.update { s -> s.copy(documents = s.documents.filterNot { it.id == id }) }
            } catch (e: Exception) {
                _state.update { it.copy(message = e.message ?: "Não foi possível remover") }
            }
        }
    }

    /** "Concluí o envio" — only once every required document is attached. */
    fun askSendDocuments() {
        val missing = _state.value.missingDocuments
        if (missing.isNotEmpty()) {
            _state.update { it.copy(message = "Envie os documentos obrigatórios antes de concluir") }
            return
        }
        ask(ConfirmKind.SEND_DOCS)
    }

    private fun sendDocuments() {
        if (_state.value.missingDocuments.isNotEmpty()) return
        run("Documentos enviados para análise") { container.applications.markDocsSent(applicationId) }
    }

    suspend fun documentUrl(doc: ClientDocument): String? =
        runCatching { container.documents.signedUrl(doc.storagePath) }
            .onFailure { e -> _state.update { it.copy(message = e.message ?: "Não foi possível abrir o arquivo") } }
            .getOrNull()

    fun messageShown() {
        _state.update { it.copy(message = null) }
    }

    private fun run(success: String, block: suspend () -> Any) {
        if (_state.value.busy) return
        _state.update { it.copy(busy = true) }
        viewModelScope.launch {
            try {
                block()
                val app = container.applications.get(applicationId)
                _state.update { it.copy(busy = false, app = app, message = success) }
            } catch (e: Exception) {
                _state.update { it.copy(busy = false, message = e.message ?: "Não foi possível concluir") }
                load()
            }
        }
    }
}
