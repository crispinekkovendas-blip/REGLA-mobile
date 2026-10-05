package br.com.imoveisregla.client.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.imoveisregla.core.data.AppContainer
import br.com.imoveisregla.core.data.SessionState
import br.com.imoveisregla.core.model.ClientDocument
import br.com.imoveisregla.core.model.ClientProfile
import br.com.imoveisregla.core.model.DocumentKind
import br.com.imoveisregla.core.model.UploadFile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private fun AppContainer.sessionEmail(): String? =
    (auth.session.value as? SessionState.SignedIn)?.session?.email

// ─── Profile tab ──────────────────────────────────────────────────────

data class ProfileUiState(
    val session: SessionState = SessionState.Loading,
    val loading: Boolean = false,
    val profile: ClientProfile? = null,
    val documentCount: Int = 0,
    val error: String? = null,
) {
    val email: String? get() = (session as? SessionState.SignedIn)?.session?.email
    val completion: Int get() = profileCompletion(profile)
}

class ProfileViewModel(private val container: AppContainer) : ViewModel() {
    private val _state = MutableStateFlow(ProfileUiState(session = container.auth.session.value))
    val state: StateFlow<ProfileUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            container.auth.session.collect { s ->
                _state.update { it.copy(session = s) }
                if (s is SessionState.SignedIn) refresh()
                else _state.update { it.copy(profile = null, documentCount = 0, loading = false, error = null) }
            }
        }
    }

    fun refresh() {
        if (container.auth.session.value !is SessionState.SignedIn) return
        viewModelScope.launch {
            _state.update { it.copy(loading = it.profile == null, error = null) }
            try {
                val profile = container.profiles.mine()
                val docs = runCatching { container.documents.list() }.getOrDefault(emptyList())
                _state.update { it.copy(loading = false, profile = profile, documentCount = docs.size) }
            } catch (e: Exception) {
                _state.update { it.copy(loading = false, error = e.message ?: "Não foi possível carregar seu perfil") }
            }
        }
    }

    fun signOut() {
        viewModelScope.launch { runCatching { container.auth.signOut() } }
    }
}

// ─── Edit profile ─────────────────────────────────────────────────────

data class EditProfileUiState(
    val loading: Boolean = true,
    val form: ProfileFormState = ProfileFormState(),
    val errors: Map<String, String> = emptyMap(),
    val saving: Boolean = false,
    val loadError: String? = null,
    val message: String? = null,
)

class EditProfileViewModel(private val container: AppContainer) : ViewModel() {
    private val _state = MutableStateFlow(EditProfileUiState())
    val state: StateFlow<EditProfileUiState> = _state.asStateFlow()
    private var triedSave = false

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, loadError = null) }
            try {
                val profile = container.profiles.mine()
                _state.update {
                    it.copy(loading = false, form = ProfileFormState.from(profile, container.sessionEmail()))
                }
            } catch (e: Exception) {
                _state.update { it.copy(loading = false, loadError = e.message ?: "Não foi possível carregar seu cadastro") }
            }
        }
    }

    fun onChange(form: ProfileFormState) {
        _state.update { it.copy(form = form, errors = if (triedSave) form.validate() else it.errors) }
    }

    fun save() {
        val form = _state.value.form
        triedSave = true
        val errors = form.validate()
        if (errors.isNotEmpty()) {
            _state.update { it.copy(errors = errors, message = "Confira os campos destacados") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(saving = true, errors = emptyMap()) }
            try {
                val saved = container.profiles.save(form.toInput())
                _state.update {
                    it.copy(saving = false, form = ProfileFormState.from(saved, container.sessionEmail()), message = "Cadastro salvo")
                }
            } catch (e: Exception) {
                _state.update { it.copy(saving = false, message = e.message ?: "Não foi possível salvar") }
            }
        }
    }

    fun consumeMessage() = _state.update { it.copy(message = null) }
}

// ─── Documents ────────────────────────────────────────────────────────

data class DocumentsUiState(
    val loading: Boolean = true,
    val documents: List<ClientDocument> = emptyList(),
    val uploadingKind: DocumentKind? = null,
    val loadError: String? = null,
    val message: String? = null,
) {
    val requiredStatus: Map<DocumentKind, Boolean> get() = requiredDocumentsStatus(documents)
    val requiredDone: Int get() = requiredStatus.count { it.value }
    val allRequiredDone: Boolean get() = hasAllRequiredDocuments(documents)
}

class DocumentsViewModel(private val container: AppContainer) : ViewModel() {
    private val _state = MutableStateFlow(DocumentsUiState())
    val state: StateFlow<DocumentsUiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(loading = it.documents.isEmpty(), loadError = null) }
            try {
                val docs = container.documents.list()
                _state.update { it.copy(loading = false, documents = docs) }
            } catch (e: Exception) {
                _state.update { it.copy(loading = false, loadError = e.message ?: "Não foi possível carregar seus documentos") }
            }
        }
    }

    fun upload(kind: DocumentKind, file: UploadFile) {
        if (file.bytes.size > MAX_DOCUMENT_BYTES) {
            _state.update { it.copy(message = "O arquivo deve ter no máximo 10 MB") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(uploadingKind = kind) }
            try {
                val row = container.documents.upload(kind, file)
                val docs = runCatching { container.documents.list() }.getOrElse { _state.value.documents + row }
                _state.update { it.copy(uploadingKind = null, documents = docs, message = "Documento enviado") }
            } catch (e: Exception) {
                _state.update { it.copy(uploadingKind = null, message = e.message ?: "Falha no envio do documento") }
            }
        }
    }

    fun delete(id: Long) {
        viewModelScope.launch {
            try {
                container.documents.delete(id)
                _state.update { s -> s.copy(documents = s.documents.filterNot { it.id == id }, message = "Documento excluído") }
            } catch (e: Exception) {
                _state.update { it.copy(message = e.message ?: "Não foi possível excluir") }
            }
        }
    }

    /** Resolves a short-lived signed URL for [doc] and hands it to [open]. */
    fun open(doc: ClientDocument, open: (String) -> Unit) {
        viewModelScope.launch {
            try {
                open(container.documents.signedUrl(doc.storagePath))
            } catch (e: Exception) {
                _state.update { it.copy(message = e.message ?: "Não foi possível abrir o documento") }
            }
        }
    }

    fun consumeMessage() = _state.update { it.copy(message = null) }
}
