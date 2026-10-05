package br.com.imoveisregla.core.data.fake

import br.com.imoveisregla.core.data.AgendaRepository
import br.com.imoveisregla.core.data.AppContainer
import br.com.imoveisregla.core.data.ApplicationRepository
import br.com.imoveisregla.core.data.AuthRepository
import br.com.imoveisregla.core.data.DashboardRepository
import br.com.imoveisregla.core.data.DocumentRepository
import br.com.imoveisregla.core.data.FavoriteRepository
import br.com.imoveisregla.core.data.LeadRepository
import br.com.imoveisregla.core.data.ListingRepository
import br.com.imoveisregla.core.data.NotAuthenticatedException
import br.com.imoveisregla.core.data.ProfileRepository
import br.com.imoveisregla.core.data.SessionState
import br.com.imoveisregla.core.data.VisitRepository
import br.com.imoveisregla.core.model.Application
import br.com.imoveisregla.core.model.ApplicationInput
import br.com.imoveisregla.core.model.ApplicationStatus
import br.com.imoveisregla.core.model.ClientDocument
import br.com.imoveisregla.core.model.ClientProfile
import br.com.imoveisregla.core.model.ClientProfileInput
import br.com.imoveisregla.core.model.DashboardStats
import br.com.imoveisregla.core.model.DocumentKind
import br.com.imoveisregla.core.model.Inquiry
import br.com.imoveisregla.core.model.InquiryStage
import br.com.imoveisregla.core.model.LeadNote
import br.com.imoveisregla.core.model.Listing
import br.com.imoveisregla.core.model.ListingFilters
import br.com.imoveisregla.core.model.ListingRef
import br.com.imoveisregla.core.model.ListingStatus
import br.com.imoveisregla.core.model.Priority
import br.com.imoveisregla.core.model.Showing
import br.com.imoveisregla.core.model.ShowingStatus
import br.com.imoveisregla.core.model.UploadFile
import br.com.imoveisregla.core.model.UserSession
import br.com.imoveisregla.core.model.VisitInput
import br.com.imoveisregla.core.model.documentPath
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.concurrent.atomic.AtomicLong

/**
 * In-memory backend: demo mode (no Supabase configured) and the default test double.
 * Mirrors the database rules that matter to the UI (RLS ownership, application → lead trigger,
 * client may only withdraw open applications).
 *
 * [asRealtor] controls what `isRealtor()` returns. [signedIn] starts with a demo session.
 */
class FakeBackend(
    private val asRealtor: Boolean,
    signedIn: Boolean = true,
    seed: Boolean = true,
) : AppContainer {

    override val isLive: Boolean = false

    private val ids = AtomicLong(1000)
    private fun nextId() = ids.incrementAndGet()
    private fun now(): String = OffsetDateTime.now(ZoneOffset.ofHours(-3)).toString()

    private val demoUser = if (asRealtor) UserSession("realtor-1", "corretor@imoveisregla.com.br")
    else UserSession("client-1", "cliente@exemplo.com")

    // ── tables ──
    val listingRows = mutableListOf<Listing>()
    val favoriteRows = mutableSetOf<Pair<String, Long>>()
    val profileRows = mutableMapOf<String, ClientProfile>()
    val applicationRows = mutableListOf<Application>()
    val documentRows = mutableListOf<ClientDocument>()
    val showingRows = mutableListOf<Showing>()
    val inquiryRows = mutableListOf<Inquiry>()
    val noteRows = mutableListOf<LeadNote>()
    private val changeEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 16)

    init {
        if (seed) SampleData.seed(this)
    }

    private fun uid(): String =
        (auth.session.value as? SessionState.SignedIn)?.session?.userId ?: throw NotAuthenticatedException()

    private fun ref(id: Long): ListingRef? = listingRows.firstOrNull { it.id == id }?.let {
        ListingRef(it.id, it.title, it.neighborhood, it.city, it.price, it.currency)
    }

    private fun emitChange() { changeEvents.tryEmit(Unit) }

    // ── auth ──
    override val auth: AuthRepository = object : AuthRepository {
        private val state = MutableStateFlow<SessionState>(
            if (signedIn) SessionState.SignedIn(demoUser) else SessionState.SignedOut,
        )
        override val session: StateFlow<SessionState> = state.asStateFlow()

        override suspend fun signIn(email: String, password: String) {
            require(email.contains("@")) { "E-mail inválido" }
            require(password.length >= 6) { "Senha incorreta" }
            state.value = SessionState.SignedIn(UserSession(demoUser.userId, email))
        }

        override suspend fun signUp(email: String, password: String) {
            require(email.contains("@")) { "E-mail inválido" }
            require(password.length >= 6) { "A senha precisa de pelo menos 6 caracteres" }
            state.value = SessionState.SignedIn(UserSession(demoUser.userId, email))
        }

        override suspend fun signOut() { state.value = SessionState.SignedOut }
        override suspend fun isRealtor(): Boolean = asRealtor && state.value is SessionState.SignedIn
    }

    // ── listings ──
    override val listings: ListingRepository = object : ListingRepository {
        override suspend fun search(filters: ListingFilters): List<Listing> = listingRows
            .filter { it.status == ListingStatus.LIVE }
            .filter { filters.city == null || it.city == filters.city }
            .filter { filters.type == null || it.type == filters.type }
            .filter { filters.minBeds == null || it.beds >= filters.minBeds }
            .filter { filters.maxPrice == null || it.price <= filters.maxPrice }
            .filter {
                val q = filters.query.trim()
                q.isEmpty() || listOf(it.title, it.neighborhood, it.city).any { f -> f.contains(q, ignoreCase = true) }
            }
            .sortedWith(compareByDescending<Listing> { it.featured }.thenByDescending { it.createdAt })

        override suspend fun byIds(ids: List<Long>): List<Listing> = listingRows.filter { it.id in ids }
        override suspend fun get(id: Long): Listing =
            listingRows.firstOrNull { it.id == id } ?: throw NoSuchElementException("Imóvel não encontrado")
        override suspend fun all(): List<Listing> = listingRows.sortedByDescending { it.updatedAt }
        override suspend fun setStatus(id: Long, status: ListingStatus) {
            val i = listingRows.indexOfFirst { it.id == id }.takeIf { it >= 0 } ?: throw NoSuchElementException()
            listingRows[i] = listingRows[i].copy(status = status, updatedAt = now())
        }
        override fun photoUrl(storagePath: String): String =
            if (storagePath.startsWith("http")) storagePath
            else "https://demo.supabase.co/storage/v1/object/public/listing-photos/$storagePath"
    }

    // ── favorites ──
    override val favorites: FavoriteRepository = object : FavoriteRepository {
        override suspend fun ids(): Set<Long> {
            val u = uid()
            return favoriteRows.filter { it.first == u }.map { it.second }.toSet()
        }
        override suspend fun set(listingId: Long, favorite: Boolean) {
            val key = uid() to listingId
            if (favorite) favoriteRows += key else favoriteRows -= key
        }
    }

    // ── profiles ──
    override val profiles: ProfileRepository = object : ProfileRepository {
        override suspend fun mine(): ClientProfile? = profileRows[uid()]
        override suspend fun save(input: ClientProfileInput): ClientProfile {
            val u = uid()
            val existing = profileRows[u]
            val row = ClientProfile(
                userId = u, fullName = input.fullName.trim(), email = input.email.trim(), phone = input.phone,
                cpf = input.cpf, birthDate = input.birthDate, occupation = input.occupation,
                employmentType = input.employmentType, monthlyIncome = input.monthlyIncome,
                residents = input.residents, hasPets = input.hasPets,
                createdAt = existing?.createdAt ?: now(), updatedAt = now(),
            )
            profileRows[u] = row
            return row
        }
    }

    // ── applications ──
    override val applications: ApplicationRepository = object : ApplicationRepository {
        override suspend fun submit(input: ApplicationInput): Application {
            val u = uid()
            val profile = profileRows[u] ?: throw IllegalStateException("Complete seu cadastro antes de enviar a proposta")
            val inquiryId = nextId()
            inquiryRows += Inquiry(
                id = inquiryId, name = profile.fullName, email = profile.email,
                intent = input.intent.name.lowercase(),
                message = "Proposta via app: R$ ${input.offeredPrice}",
                propertyId = input.listingId, stage = InquiryStage.OFFER, priority = Priority.HIGH,
                lastActivityAt = now(), createdAt = now(), listings = ref(input.listingId),
            )
            val row = Application(
                id = nextId(), listingId = input.listingId, userId = u, intent = input.intent,
                offeredPrice = input.offeredPrice, guaranteeType = input.guaranteeType,
                moveInDate = input.moveInDate, message = input.message, inquiryId = inquiryId,
                createdAt = now(), updatedAt = now(), listings = ref(input.listingId), profile = profile,
            )
            applicationRows += row
            emitChange()
            return row
        }

        override suspend fun mine(): List<Application> {
            val u = uid()
            return applicationRows.filter { it.userId == u }.sortedByDescending { it.createdAt }
        }

        override suspend fun withdraw(id: Long) {
            val u = uid()
            val i = applicationRows.indexOfFirst { it.id == id && it.userId == u }
            check(i >= 0) { "Proposta não encontrada" }
            check(applicationRows[i].status.isOpen) { "Esta proposta não pode mais ser cancelada" }
            applicationRows[i] = applicationRows[i].copy(status = ApplicationStatus.WITHDRAWN, updatedAt = now())
        }

        override suspend fun forReview(status: ApplicationStatus?): List<Application> = applicationRows
            .filter { status == null || it.status == status }
            .map { it.copy(profile = profileRows[it.userId], listings = ref(it.listingId)) }
            .sortedByDescending { it.createdAt }

        override suspend fun get(id: Long): Application =
            applicationRows.firstOrNull { it.id == id }
                ?.let { it.copy(profile = profileRows[it.userId], listings = ref(it.listingId)) }
                ?: throw NoSuchElementException("Proposta não encontrada")

        override suspend fun review(id: Long, status: ApplicationStatus, note: String?) {
            val i = applicationRows.indexOfFirst { it.id == id }
            check(i >= 0) { "Proposta não encontrada" }
            val app = applicationRows[i]
            applicationRows[i] = app.copy(status = status, reviewerNote = note, reviewedBy = uid(), updatedAt = now())
            if (status == ApplicationStatus.APPROVED) {
                val li = inquiryRows.indexOfFirst { it.id == app.inquiryId }
                if (li >= 0) inquiryRows[li] = inquiryRows[li].copy(stage = InquiryStage.CLOSED_WON)
            }
        }
    }

    // ── documents ──
    override val documents: DocumentRepository = object : DocumentRepository {
        override suspend fun upload(kind: DocumentKind, file: UploadFile, applicationId: Long?): ClientDocument {
            val u = uid()
            val row = ClientDocument(
                id = nextId(), userId = u, applicationId = applicationId, kind = kind, filename = file.name,
                storagePath = documentPath(u, kind, file.name), mimeType = file.mimeType,
                sizeBytes = file.bytes.size.toLong().coerceAtLeast(1), createdAt = now(),
            )
            documentRows += row
            return row
        }
        override suspend fun list(userId: String?): List<ClientDocument> {
            val who = userId ?: uid()
            return documentRows.filter { it.userId == who }.sortedByDescending { it.createdAt }
        }
        override suspend fun delete(id: Long) {
            val u = uid()
            documentRows.removeAll { it.id == id && it.userId == u }
        }
        override suspend fun attachToApplication(documentIds: List<Long>, applicationId: Long) {
            val u = uid()
            documentRows.replaceAll { if (it.id in documentIds && it.userId == u) it.copy(applicationId = applicationId) else it }
        }
        override suspend fun signedUrl(storagePath: String, seconds: Int): String =
            "https://demo.supabase.co/storage/v1/object/sign/client-documents/$storagePath?token=demo"
    }

    // ── visits ──
    override val visits: VisitRepository = object : VisitRepository {
        override suspend fun book(input: VisitInput) {
            showingRows += Showing(
                id = nextId(), listingId = input.listingId, startsAt = input.startsAt,
                visitorName = input.visitorName, visitorEmail = input.visitorEmail,
                visitorPhone = input.visitorPhone, notes = input.notes, createdBy = uid(),
                listings = ref(input.listingId),
            )
            emitChange()
        }
        override suspend fun mine(): List<Showing> {
            val u = uid()
            return showingRows.filter { it.createdBy == u }.sortedBy { it.startsAt }
        }
        override suspend fun cancel(id: Long) {
            val u = uid()
            showingRows.replaceAll { if (it.id == id && it.createdBy == u) it.copy(status = ShowingStatus.CANCELLED) else it }
        }
    }

    // ── leads ──
    override val leads: LeadRepository = object : LeadRepository {
        override suspend fun list(stage: InquiryStage?): List<Inquiry> = inquiryRows
            .filter { stage == null || it.stage == stage }
            .sortedByDescending { it.lastActivityAt }
        override suspend fun get(id: Long): Inquiry =
            inquiryRows.firstOrNull { it.id == id } ?: throw NoSuchElementException("Lead não encontrado")
        override suspend fun update(id: Long, stage: InquiryStage?, priority: Priority?, read: Boolean?) {
            val i = inquiryRows.indexOfFirst { it.id == id }
            check(i >= 0) { "Lead não encontrado" }
            val l = inquiryRows[i]
            inquiryRows[i] = l.copy(
                stage = stage ?: l.stage, priority = priority ?: l.priority, read = read ?: l.read, lastActivityAt = now(),
            )
        }
        override suspend fun notes(inquiryId: Long): List<LeadNote> =
            noteRows.filter { it.inquiryId == inquiryId }.sortedByDescending { it.createdAt }
        override suspend fun addNote(inquiryId: Long, body: String): LeadNote {
            require(body.isNotBlank()) { "Escreva a nota" }
            val n = LeadNote(nextId(), inquiryId, body.trim(), uid(), now())
            noteRows += n
            return n
        }
    }

    // ── agenda ──
    override val agenda: AgendaRepository = object : AgendaRepository {
        override suspend fun range(fromIso: String, toIso: String): List<Showing> {
            val from = OffsetDateTime.parse(fromIso)
            val to = OffsetDateTime.parse(toIso)
            return showingRows.filter {
                val t = OffsetDateTime.parse(it.startsAt)
                !t.isBefore(from) && t.isBefore(to)
            }.sortedBy { OffsetDateTime.parse(it.startsAt) }
        }
        override suspend fun setStatus(id: Long, status: ShowingStatus) {
            showingRows.replaceAll { if (it.id == id) it.copy(status = status) else it }
        }
    }

    // ── dashboard ──
    override val dashboard: DashboardRepository = object : DashboardRepository {
        override suspend fun stats(): DashboardStats {
            val today = OffsetDateTime.now(ZoneOffset.ofHours(-3)).toLocalDate()
            return DashboardStats(
                newLeads = inquiryRows.count { it.stage == InquiryStage.INBOX },
                pendingApplications = applicationRows.count {
                    it.status == ApplicationStatus.SUBMITTED || it.status == ApplicationStatus.UNDER_REVIEW
                },
                visitsToday = showingRows.count { OffsetDateTime.parse(it.startsAt).toLocalDate() == today },
                liveListings = listingRows.count { it.status == ListingStatus.LIVE },
            )
        }
        override fun changes(): Flow<Unit> = changeEvents.asSharedFlow()
    }

    internal fun newId(): Long = nextId()
    internal fun listingRef(id: Long): ListingRef? = ref(id)
}
