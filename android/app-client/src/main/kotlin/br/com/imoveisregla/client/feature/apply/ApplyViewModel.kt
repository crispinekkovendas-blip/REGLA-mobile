package br.com.imoveisregla.client.feature.apply

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.imoveisregla.core.data.AppContainer
import br.com.imoveisregla.core.data.SessionState
import br.com.imoveisregla.core.model.Application
import br.com.imoveisregla.core.model.ApplicationInput
import br.com.imoveisregla.core.model.ApplicationIntent
import br.com.imoveisregla.core.model.ClientDocument
import br.com.imoveisregla.core.model.ClientProfile
import br.com.imoveisregla.core.model.ClientProfileInput
import br.com.imoveisregla.core.model.DocumentKind
import br.com.imoveisregla.core.model.EmploymentType
import br.com.imoveisregla.core.model.GuaranteeType
import br.com.imoveisregla.core.model.Listing
import br.com.imoveisregla.core.model.UploadFile
import br.com.imoveisregla.core.model.digitsOnly
import br.com.imoveisregla.core.model.maskCpf
import br.com.imoveisregla.core.model.maskPhoneBR
import br.com.imoveisregla.core.model.validateApplication
import br.com.imoveisregla.core.model.validateProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

enum class WizardStep(val title: String) {
    PROFILE("Cadastro"),
    OFFER("Proposta"),
    DOCUMENTS("Documentos"),
    REVIEW("Revisão"),
}

/** Documents a proposal needs before the realtor can analyse it. */
val REQUIRED_DOCUMENTS: List<DocumentKind> = listOf(
    DocumentKind.RG_CNH,
    DocumentKind.COMPROVANTE_RENDA,
    DocumentKind.COMPROVANTE_RESIDENCIA,
)

/** Optional extras offered in the checklist. */
val OPTIONAL_DOCUMENTS: List<DocumentKind> = listOf(
    DocumentKind.CPF,
    DocumentKind.EXTRATO_BANCARIO,
    DocumentKind.IMPOSTO_RENDA,
    DocumentKind.OUTRO,
)

const val MAX_UPLOAD_BYTES: Long = 10L * 1024 * 1024
const val MAX_MESSAGE_LENGTH = 2000

/** Step-1 draft. Phone / CPF / income are kept as digits; the UI masks them. */
data class WizardProfileDraft(
    val fullName: String = "",
    val email: String = "",
    val phoneDigits: String = "",
    val cpfDigits: String = "",
    val employmentType: EmploymentType? = null,
    val incomeDigits: String = "",
    val residents: Int = 1,
    val hasPets: Boolean = false,
    // carried over untouched from an existing profile
    val birthDate: String? = null,
    val occupation: String? = null,
) {
    val monthlyIncome: Long? get() = incomeDigits.toLongOrNull()

    fun toInput(): ClientProfileInput = ClientProfileInput(
        fullName = fullName.trim(),
        email = email.trim(),
        phone = maskPhoneBR(phoneDigits),
        cpf = cpfDigits.takeIf { it.isNotBlank() }?.let { maskCpf(it) },
        birthDate = birthDate,
        occupation = occupation,
        employmentType = employmentType,
        monthlyIncome = monthlyIncome,
        residents = residents,
        hasPets = hasPets,
    )

    companion object {
        fun from(profile: ClientProfile?, sessionEmail: String?): WizardProfileDraft {
            if (profile == null) return WizardProfileDraft(email = sessionEmail.orEmpty())
            val income = profile.monthlyIncome
            return WizardProfileDraft(
                fullName = profile.fullName,
                email = profile.email.ifBlank { sessionEmail.orEmpty() },
                phoneDigits = digitsOnly(profile.phone).take(11),
                cpfDigits = digitsOnly(profile.cpf.orEmpty()).take(11),
                employmentType = profile.employmentType,
                incomeDigits = if (income != null && income > 0) income.toString() else "",
                residents = profile.residents.coerceIn(1, 20),
                hasPets = profile.hasPets,
                birthDate = profile.birthDate,
                occupation = profile.occupation,
            )
        }
    }
}

/** Step-2 draft. */
data class OfferDraft(
    val intent: ApplicationIntent = ApplicationIntent.RENT,
    val priceDigits: String = "",
    val guaranteeType: GuaranteeType? = null,
    val moveInDate: String = "",
    val message: String = "",
) {
    val offeredPrice: Long get() = priceDigits.toLongOrNull() ?: 0L

    fun toInput(listingId: Long): ApplicationInput = ApplicationInput(
        listingId = listingId,
        intent = intent,
        offeredPrice = offeredPrice,
        guaranteeType = if (intent == ApplicationIntent.RENT) guaranteeType else null,
        moveInDate = moveInDate.takeIf { it.isNotBlank() },
        message = message.trim().takeIf { it.isNotBlank() },
    )
}

data class ApplyUiState(
    val loading: Boolean = true,
    val loadError: String? = null,
    val signedOut: Boolean = false,
    val listing: Listing? = null,
    val existingOpen: Application? = null,
    val step: WizardStep = WizardStep.PROFILE,
    val profile: WizardProfileDraft = WizardProfileDraft(),
    val profileErrors: Map<String, String> = emptyMap(),
    val savedProfile: ClientProfile? = null,
    val offer: OfferDraft = OfferDraft(),
    val offerErrors: Map<String, String> = emptyMap(),
    /** Every document the user has on file (existing + uploaded in this wizard). */
    val documents: List<ClientDocument> = emptyList(),
    /** Ids uploaded during this wizard; attached to the application on submit. */
    val uploadedIds: List<Long> = emptyList(),
    val uploadingKind: DocumentKind? = null,
    val documentError: String? = null,
    val showSkipDocsDialog: Boolean = false,
    val docsSkipped: Boolean = false,
    val declared: Boolean = false,
    val declarationError: Boolean = false,
    val busy: Boolean = false,
    val error: String? = null,
    val submitted: Application? = null,
    val attachWarning: String? = null,
) {
    val missingRequired: List<DocumentKind>
        get() = REQUIRED_DOCUMENTS.filter { kind -> documents.none { it.kind == kind } }

    val monthlyIncome: Long? get() = profile.monthlyIncome ?: savedProfile?.monthlyIncome
}

/** Signed % difference of [offered] vs the [asking] price, rounded (e.g. -4). */
fun priceDifferencePercent(offered: Long, asking: Long): Int? {
    if (asking <= 0 || offered <= 0) return null
    return ((offered - asking).toDouble() * 100.0 / asking).roundToInt()
}

fun priceDifferenceLabel(offered: Long, asking: Long): String? {
    val pct = priceDifferencePercent(offered, asking) ?: return null
    return when {
        pct == 0 && offered == asking -> "Mesmo valor do anúncio"
        pct > 0 -> "Diferença do anúncio: +$pct%"
        else -> "Diferença do anúncio: $pct%"
    }
}

/** Returns an error message when the picked file can't be uploaded, null when fine. */
fun uploadProblem(mimeType: String?, sizeBytes: Long): String? = when {
    sizeBytes <= 0 -> "Arquivo vazio ou ilegível"
    sizeBytes > MAX_UPLOAD_BYTES -> "Arquivo maior que 10 MB"
    mimeType == null || !(mimeType == "application/pdf" || mimeType.startsWith("image/")) -> "Envie um PDF ou uma imagem"
    else -> null
}

/** Step-2 validation: core rules + a guarantee is required for rentals. */
fun validateOffer(offer: OfferDraft, listingId: Long): Map<String, String> {
    val errors = validateApplication(offer.toInput(listingId)).toMutableMap()
    if (offer.intent == ApplicationIntent.RENT && offer.guaranteeType == null) {
        errors["guaranteeType"] = "Escolha uma garantia"
    }
    return errors
}

class ApplyViewModel(
    private val listingId: Long,
    private val container: AppContainer,
) : ViewModel() {

    private val _state = MutableStateFlow(ApplyUiState())
    val state: StateFlow<ApplyUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        _state.update { it.copy(loading = true, loadError = null) }
        viewModelScope.launch {
            val session = container.auth.session.value
            if (session !is SessionState.SignedIn) {
                _state.update { it.copy(loading = false, signedOut = true) }
                return@launch
            }
            try {
                val listing = container.listings.get(listingId)
                val profile = container.profiles.mine()
                val docs = runCatching { container.documents.list() }.getOrDefault(emptyList())
                val mine = runCatching { container.applications.mine() }.getOrDefault(emptyList())
                val open = mine.firstOrNull { it.listingId == listingId && it.status.isOpen }
                _state.update {
                    it.copy(
                        loading = false,
                        signedOut = false,
                        listing = listing,
                        existingOpen = open,
                        profile = WizardProfileDraft.from(profile, session.session.email),
                        savedProfile = profile,
                        offer = OfferDraft(
                            intent = if (listing.isRental) ApplicationIntent.RENT else ApplicationIntent.BUY,
                            priceDigits = listing.price.takeIf { p -> p > 0 }?.toString().orEmpty(),
                        ),
                        documents = docs,
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(loading = false, loadError = e.message ?: "Não foi possível carregar o imóvel") }
            }
        }
    }

    // ── step 1 ──
    fun updateProfile(transform: (WizardProfileDraft) -> WizardProfileDraft) {
        _state.update { s ->
            val next = transform(s.profile)
            s.copy(profile = next, profileErrors = if (s.profileErrors.isEmpty()) s.profileErrors else
                validateProfile(next.toInput(), requireFinancials = true))
        }
    }

    // ── step 2 ──
    fun updateOffer(transform: (OfferDraft) -> OfferDraft) {
        _state.update { s ->
            val raw = transform(s.offer)
            val next = raw.copy(message = raw.message.take(MAX_MESSAGE_LENGTH))
            s.copy(offer = next, offerErrors = if (s.offerErrors.isEmpty()) s.offerErrors else validateOffer(next, listingId))
        }
    }

    // ── step 3 ──
    fun upload(kind: DocumentKind, file: UploadFile) {
        val problem = uploadProblem(file.mimeType, file.bytes.size.toLong())
        if (problem != null) {
            _state.update { it.copy(documentError = problem) }
            return
        }
        _state.update { it.copy(uploadingKind = kind, documentError = null) }
        viewModelScope.launch {
            try {
                val doc = container.documents.upload(kind, file, applicationId = null)
                _state.update {
                    it.copy(
                        uploadingKind = null,
                        documents = listOf(doc) + it.documents,
                        uploadedIds = it.uploadedIds + doc.id,
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(uploadingKind = null, documentError = e.message ?: "Falha no envio do arquivo") }
            }
        }
    }

    fun reportDocumentError(message: String) {
        _state.update { it.copy(documentError = message, uploadingKind = null) }
    }

    fun removeUploaded(id: Long) {
        if (id !in _state.value.uploadedIds) return
        viewModelScope.launch {
            try {
                container.documents.delete(id)
                _state.update { s -> s.copy(documents = s.documents.filterNot { it.id == id }, uploadedIds = s.uploadedIds - id) }
            } catch (e: Exception) {
                _state.update { it.copy(documentError = e.message ?: "Não foi possível remover") }
            }
        }
    }

    fun skipDocuments() {
        _state.update { it.copy(showSkipDocsDialog = true) }
    }

    fun dismissSkipDialog() {
        _state.update { it.copy(showSkipDocsDialog = false) }
    }

    fun confirmSkipDocuments() {
        _state.update { it.copy(showSkipDocsDialog = false, docsSkipped = true, step = WizardStep.REVIEW) }
    }

    // ── step 4 ──
    fun setDeclared(value: Boolean) {
        _state.update { it.copy(declared = value, declarationError = false) }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }

    // ── navigation ──
    fun back(): Boolean {
        val s = _state.value
        if (s.busy || s.submitted != null) return false
        val prev = WizardStep.entries.getOrNull(s.step.ordinal - 1) ?: return false
        _state.update { it.copy(step = prev, error = null) }
        return true
    }

    fun next() {
        val s = _state.value
        if (s.busy || s.listing == null) return
        when (s.step) {
            WizardStep.PROFILE -> submitProfile()
            WizardStep.OFFER -> {
                val errors = validateOffer(s.offer, listingId)
                _state.update { it.copy(offerErrors = errors) }
                if (errors.isEmpty()) _state.update { it.copy(step = WizardStep.DOCUMENTS) }
            }
            WizardStep.DOCUMENTS -> {
                if (s.missingRequired.isEmpty()) _state.update { it.copy(step = WizardStep.REVIEW, docsSkipped = false) }
                else _state.update { it.copy(showSkipDocsDialog = true) }
            }
            WizardStep.REVIEW -> submit()
        }
    }

    private fun submitProfile() {
        val input = _state.value.profile.toInput()
        val errors = validateProfile(input, requireFinancials = true)
        _state.update { it.copy(profileErrors = errors) }
        if (errors.isNotEmpty()) return
        _state.update { it.copy(busy = true, error = null) }
        viewModelScope.launch {
            try {
                val saved = container.profiles.save(input)
                _state.update { it.copy(busy = false, savedProfile = saved, step = WizardStep.OFFER) }
            } catch (e: Exception) {
                _state.update { it.copy(busy = false, error = e.message ?: "Não foi possível salvar seu cadastro") }
            }
        }
    }

    private fun submit() {
        val s = _state.value
        if (!s.declared) {
            _state.update { it.copy(declarationError = true) }
            return
        }
        val input = s.offer.toInput(listingId)
        val errors = validateOffer(s.offer, listingId)
        if (errors.isNotEmpty()) {
            _state.update { it.copy(offerErrors = errors, step = WizardStep.OFFER) }
            return
        }
        _state.update { it.copy(busy = true, error = null) }
        viewModelScope.launch {
            val app = try {
                container.applications.submit(input)
            } catch (e: Exception) {
                _state.update { it.copy(busy = false, error = e.message ?: "Não foi possível enviar a proposta") }
                return@launch
            }
            var warning: String? = null
            val ids = _state.value.uploadedIds
            if (ids.isNotEmpty()) {
                try {
                    container.documents.attachToApplication(ids, app.id)
                } catch (e: Exception) {
                    warning = "A proposta foi enviada, mas não conseguimos vincular os documentos. Envie-os em Perfil › Documentos."
                }
            }
            _state.update { it.copy(busy = false, submitted = app, attachWarning = warning) }
        }
    }
}
