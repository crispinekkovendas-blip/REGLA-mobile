package br.com.imoveisregla.core.data.supabase

import br.com.imoveisregla.core.data.AgendaRepository
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
import br.com.imoveisregla.core.model.GuaranteeType
import br.com.imoveisregla.core.model.Inquiry
import br.com.imoveisregla.core.model.InquiryStage
import br.com.imoveisregla.core.model.LeadNote
import br.com.imoveisregla.core.model.Listing
import br.com.imoveisregla.core.model.ListingFilters
import br.com.imoveisregla.core.model.ListingStatus
import br.com.imoveisregla.core.model.Offer
import br.com.imoveisregla.core.model.Priority
import br.com.imoveisregla.core.model.Showing
import br.com.imoveisregla.core.model.ShowingStatus
import br.com.imoveisregla.core.model.UploadFile
import br.com.imoveisregla.core.model.UserSession
import br.com.imoveisregla.core.model.VisitInput
import br.com.imoveisregla.core.model.documentPath
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.providers.builtin.IDToken
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Count
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.query.filter.PostgrestFilterBuilder
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import io.github.jan.supabase.storage.storage
import io.ktor.http.ContentType
import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration.Companion.seconds

// PostgREST select strings. Embeds use explicit FK hints: listings is reachable from
// inquiries / showings / applications through more than one path (applications and
// showings both reference inquiries AND listings), which makes un-hinted embeds ambiguous.
internal object Selects {
    const val LISTING = "*,listing_photos(id,listing_id,storage_path,alt_text,position)"
    private const val REF = "id,title,neighborhood,city,price,currency"
    const val APPLICATION = "*,listings!applications_listing_id_fkey($REF),application_offers(*)"
    const val APPLICATION_REVIEW = "$APPLICATION,client_profiles!applications_user_id_fkey(*)"
    const val SHOWING = "*,listings!showings_listing_id_fkey($REF)"
    const val INQUIRY = "*,listings!inquiries_property_id_fkey($REF)"
}

internal const val CLIENT_DOCUMENTS_BUCKET = "client-documents"
internal const val LISTING_PHOTOS_BUCKET = "listing-photos"
internal val BRT: ZoneId = ZoneId.of("America/Sao_Paulo")

/** Supplies the signed-in user's id (auth.uid()); null when signed out. */
internal fun interface CurrentUser {
    fun idOrNull(): String?
}

internal fun CurrentUser.require(): String = idOrNull() ?: throw NotAuthenticatedException()

/** Strips characters that would break a PostgREST `or=(…)` / ilike expression. */
internal fun sanitizeQuery(q: String): String =
    q.replace(Regex("[%,()\"\\\\*:]"), " ").replace(Regex("\\s+"), " ").trim()

private fun Listing.sortedPhotos(): Listing = copy(photos = photos.sortedBy { it.position })

private fun Application.sortedOffers(): Application = copy(offers = offers.sortedWith(compareBy({ it.createdAt }, { it.id })))

// ─── auth ─────────────────────────────────────────────────────────────

internal class SupabaseAuthRepository(
    private val client: SupabaseClient,
    scope: CoroutineScope,
) : AuthRepository {

    override val session: StateFlow<SessionState> = client.auth.sessionStatus
        .map { toState(it) }
        .stateIn(scope, SharingStarted.Eagerly, toState(client.auth.sessionStatus.value))

    private fun toState(status: SessionStatus): SessionState = when (status) {
        is SessionStatus.Initializing -> SessionState.Loading
        is SessionStatus.NotAuthenticated -> SessionState.SignedOut
        is SessionStatus.Authenticated -> status.session.user
            ?.let { SessionState.SignedIn(UserSession(it.id, it.email.orEmpty())) }
            ?: SessionState.SignedOut
        // Token refresh failed (usually offline) — keep the user signed in while a session exists.
        is SessionStatus.RefreshFailure -> client.auth.currentUserOrNull()
            ?.let { SessionState.SignedIn(UserSession(it.id, it.email.orEmpty())) }
            ?: SessionState.SignedOut
    }

    override suspend fun signIn(email: String, password: String) {
        val e = email.trim()
        remote {
            client.auth.signInWith(Email) {
                this.email = e
                this.password = password
            }
        }
    }

    override suspend fun signInWithGoogle(idToken: String, rawNonce: String) {
        remote {
            client.auth.signInWith(IDToken) {
                this.idToken = idToken
                provider = Google
                nonce = rawNonce
            }
        }
    }

    override suspend fun signUp(email: String, password: String) {
        val e = email.trim()
        remote {
            client.auth.signUpWith(Email) {
                this.email = e
                this.password = password
            }
        }
        if (client.auth.currentSessionOrNull() == null) {
            throw ReglaDataException("Enviamos um link de confirmação para $e. Confirme seu e-mail e depois faça login.")
        }
    }

    override suspend fun signOut() {
        try {
            client.auth.signOut()
        } catch (e: kotlin.coroutines.cancellation.CancellationException) {
            throw e
        } catch (_: Throwable) {
            client.auth.clearSession() // offline: drop the local session anyway
        }
    }

    override suspend fun isRealtor(): Boolean {
        if (client.auth.currentUserOrNull() == null) return false
        return try {
            client.postgrest.rpc("is_admin").decodeAs<Boolean>()
        } catch (e: kotlin.coroutines.cancellation.CancellationException) {
            throw e
        } catch (_: Throwable) {
            false
        }
    }
}

// ─── listings ─────────────────────────────────────────────────────────

internal class SupabaseListingRepository(
    private val client: SupabaseClient,
    supabaseUrl: String,
) : ListingRepository {

    private val publicBase = supabaseUrl.trimEnd('/') + "/storage/v1/object/public/$LISTING_PHOTOS_BUCKET/"

    override suspend fun search(filters: ListingFilters): List<Listing> = remote {
        val q = sanitizeQuery(filters.query)
        client.from("listings").select(Columns.raw(Selects.LISTING)) {
            filter {
                eq("status", wire(ListingStatus.LIVE))
                filters.city?.takeIf { it.isNotBlank() }?.let { eq("city", it) }
                filters.type?.let { eq("type", wire(it)) }
                filters.minBeds?.takeIf { it > 0 }?.let { gte("beds", it) }
                filters.maxPrice?.takeIf { it > 0 }?.let { lte("price", it) }
                if (q.isNotEmpty()) {
                    or {
                        ilike("title", "%$q%")
                        ilike("neighborhood", "%$q%")
                        ilike("city", "%$q%")
                    }
                }
            }
            order("featured", Order.DESCENDING)
            order("created_at", Order.DESCENDING)
            limit(100)
        }.decodeList<Listing>().map { it.sortedPhotos() }
    }

    override suspend fun byIds(ids: List<Long>): List<Listing> {
        if (ids.isEmpty()) return emptyList()
        return remote {
            client.from("listings").select(Columns.raw(Selects.LISTING)) {
                filter { isIn("id", ids.distinct()) }
            }.decodeList<Listing>().map { it.sortedPhotos() }
        }
    }

    override suspend fun get(id: Long): Listing = remote {
        client.from("listings").select(Columns.raw(Selects.LISTING)) {
            filter { eq("id", id) }
        }.decodeSingleOrNull<Listing>()?.sortedPhotos()
    } ?: throw NoSuchElementException("Imóvel não encontrado")

    override suspend fun all(): List<Listing> = remote {
        client.from("listings").select(Columns.raw(Selects.LISTING)) {
            order("updated_at", Order.DESCENDING)
        }.decodeList<Listing>().map { it.sortedPhotos() }
    }

    override suspend fun setStatus(id: Long, status: ListingStatus) {
        val rows = remote {
            client.from("listings").update(ListingStatusPatch(status)) {
                select(Columns.raw("id"))
                filter { eq("id", id) }
            }.decodeList<IdRow>()
        }
        if (rows.isEmpty()) throw NoSuchElementException("Imóvel não encontrado")
    }

    override fun photoUrl(storagePath: String): String =
        if (storagePath.startsWith("http://") || storagePath.startsWith("https://")) storagePath
        else publicBase + storagePath.trimStart('/')
}

// ─── favorites ────────────────────────────────────────────────────────

internal class SupabaseFavoriteRepository(
    private val client: SupabaseClient,
    private val user: CurrentUser,
) : FavoriteRepository {

    override suspend fun ids(): Set<Long> {
        val uid = user.require()
        return remote {
            client.from("favorites").select(Columns.raw("listing_id")) {
                filter { eq("user_id", uid) }
            }.decodeList<FavoriteRow>().map { it.listingId }.toSet()
        }
    }

    override suspend fun set(listingId: Long, favorite: Boolean) {
        val uid = user.require()
        remote {
            if (favorite) {
                client.from("favorites").upsert(FavoriteInsert(uid, listingId)) {
                    onConflict = "user_id,listing_id"
                    ignoreDuplicates = true
                }
            } else {
                client.from("favorites").delete {
                    filter {
                        eq("user_id", uid)
                        eq("listing_id", listingId)
                    }
                }
            }
        }
    }
}

// ─── profiles ─────────────────────────────────────────────────────────

internal class SupabaseProfileRepository(
    private val client: SupabaseClient,
    private val user: CurrentUser,
) : ProfileRepository {

    override suspend fun mine(): ClientProfile? {
        val uid = user.require()
        return remote {
            client.from("client_profiles").select {
                filter { eq("user_id", uid) }
            }.decodeSingleOrNull<ClientProfile>()
        }
    }

    override suspend fun save(input: ClientProfileInput): ClientProfile {
        val uid = user.require()
        return remote {
            client.from("client_profiles").upsert(ProfileUpsert.from(uid, input)) {
                onConflict = "user_id"
                select()
            }.decodeSingle<ClientProfile>()
        }
    }
}

// ─── applications ("propostas") ───────────────────────────────────────

internal class SupabaseApplicationRepository(
    private val client: SupabaseClient,
    private val user: CurrentUser,
) : ApplicationRepository {

    override suspend fun submit(input: ApplicationInput): Application {
        val uid = user.require()
        return remote {
            // applications.user_id references client_profiles: a profile must exist first.
            val hasProfile = client.from("client_profiles").select(Columns.raw("user_id")) {
                filter { eq("user_id", uid) }
            }.data.trim().let { it.isNotEmpty() && it != "[]" }
            if (!hasProfile) throw ReglaDataException("Complete seu cadastro antes de enviar a proposta")
            client.from("applications").insert(ApplicationInsert.from(uid, input)) {
                select(Columns.raw(Selects.APPLICATION))
            }.decodeSingle<Application>().sortedOffers()
        }
    }

    override suspend fun mine(): List<Application> {
        val uid = user.require()
        return remote {
            client.from("applications").select(Columns.raw(Selects.APPLICATION)) {
                filter { eq("user_id", uid) }
                order("created_at", Order.DESCENDING)
            }.decodeList<Application>().map { it.sortedOffers() }
        }
    }

    override suspend fun withdraw(id: Long) {
        val uid = user.require()
        val rows = remote {
            client.from("applications").update(ApplicationStatusPatch(ApplicationStatus.WITHDRAWN)) {
                select(Columns.raw("id"))
                filter {
                    eq("id", id)
                    eq("user_id", uid)
                }
            }.decodeList<IdRow>()
        }
        if (rows.isEmpty()) throw ReglaDataException("Proposta não encontrada")
    }

    override suspend fun forReview(status: ApplicationStatus?): List<Application> = remote {
        client.from("applications").select(Columns.raw(Selects.APPLICATION_REVIEW)) {
            if (status != null) filter { eq("status", wire(status)) }
            order("created_at", Order.DESCENDING)
        }.decodeList<Application>().map { it.sortedOffers() }
    }

    override suspend fun get(id: Long): Application {
        user.require()
        return remote {
            // Clients can't read client_profiles of others, but their own embed resolves fine.
            client.from("applications").select(Columns.raw(Selects.APPLICATION_REVIEW)) {
                filter { eq("id", id) }
            }.decodeSingleOrNull<Application>()?.sortedOffers()
        } ?: throw NoSuchElementException("Proposta não encontrada")
    }

    override suspend fun counter(
        id: Long,
        price: Long,
        message: String?,
        guaranteeType: GuaranteeType?,
        moveInDate: String?,
    ): Offer {
        user.require()
        val params = buildJsonObject {
            put("p_application_id", id)
            put("p_price", price)
            put("p_message", message.blankToNull())
            put("p_guarantee_type", guaranteeType?.let { wire(it) })
            put("p_move_in_date", moveInDate.blankToNull())
        }
        return remote { client.postgrest.rpc("proposal_counter", params).decodeAs<Offer>() }
    }

    override suspend fun accept(id: Long): Application = transition("proposal_accept", id)

    override suspend fun decline(id: Long, note: String?): Application =
        transition("proposal_decline", id) { put("p_note", note.blankToNull()) }

    override suspend fun markDocsSent(id: Long): Application = transition("proposal_docs_sent", id)

    /** Calls a lifecycle RPC, then re-reads the row with its embeds. */
    private suspend fun transition(fn: String, id: Long, extra: JsonObjectBuilder.() -> Unit = {}): Application {
        user.require()
        val params = buildJsonObject {
            put("p_application_id", id)
            extra()
        }
        remote { client.postgrest.rpc(fn, params) }
        return get(id)
    }

    override suspend fun review(id: Long, status: ApplicationStatus, note: String?) {
        val uid = user.require()
        val rows = remote {
            client.from("applications").update(ApplicationReviewPatch(status, note.blankToNull(), uid)) {
                select(Columns.raw("id"))
                filter { eq("id", id) }
            }.decodeList<IdRow>()
        }
        if (rows.isEmpty()) throw ReglaDataException("Proposta não encontrada")
    }
}

// ─── documents ────────────────────────────────────────────────────────

internal class SupabaseDocumentRepository(
    private val client: SupabaseClient,
    private val user: CurrentUser,
) : DocumentRepository {

    private val bucket get() = client.storage.from(CLIENT_DOCUMENTS_BUCKET)

    override suspend fun upload(kind: DocumentKind, file: UploadFile, applicationId: Long?): ClientDocument {
        val uid = user.require()
        require(file.bytes.isNotEmpty()) { "Arquivo vazio" }
        val path = documentPath(uid, kind, file.name)
        val type = runCatching { ContentType.parse(file.mimeType) }.getOrDefault(ContentType.Application.OctetStream)
        remote {
            bucket.upload(path, file.bytes) {
                upsert = false
                contentType = type
            }
        }
        return try {
            remote {
                client.from("client_documents").insert(
                    DocumentInsert(uid, applicationId, kind, file.name, path, file.mimeType, file.bytes.size.toLong()),
                ) { select() }.decodeSingle<ClientDocument>()
            }
        } catch (e: Throwable) {
            withContext(NonCancellable) { runCatching { bucket.delete(path) } } // don't leave orphans
            throw e
        }
    }

    override suspend fun list(userId: String?): List<ClientDocument> {
        val who = userId ?: user.require()
        return remote {
            client.from("client_documents").select {
                filter { eq("user_id", who) }
                order("created_at", Order.DESCENDING)
            }.decodeList<ClientDocument>()
        }
    }

    override suspend fun delete(id: Long) {
        val uid = user.require()
        remote {
            val doc = client.from("client_documents").select {
                filter {
                    eq("id", id)
                    eq("user_id", uid)
                }
            }.decodeSingleOrNull<ClientDocument>() ?: return@remote
            client.from("client_documents").delete {
                filter {
                    eq("id", id)
                    eq("user_id", uid)
                }
            }
            runCatching { bucket.delete(doc.storagePath) }
            Unit
        }
    }

    // Plain UPDATE: migration 0013 lets clients link their own documents to their own proposta.
    override suspend fun attachToApplication(documentIds: List<Long>, applicationId: Long) {
        val uid = user.require()
        if (documentIds.isEmpty()) return
        remote {
            client.from("client_documents").update(DocumentApplicationPatch(applicationId)) {
                filter {
                    isIn("id", documentIds.distinct())
                    eq("user_id", uid)
                }
            }
            Unit
        }
    }

    override suspend fun signedUrl(storagePath: String, seconds: Int): String = remote {
        bucket.createSignedUrl(storagePath, seconds.coerceAtLeast(1).seconds)
    }
}

// ─── visits (client) ──────────────────────────────────────────────────

internal class SupabaseVisitRepository(
    private val client: SupabaseClient,
    private val user: CurrentUser,
) : VisitRepository {

    override suspend fun book(input: VisitInput) {
        val uid = user.require()
        // Policy "showings: visitor books own": created_by = uid, type private, status scheduled.
        remote { client.from("showings").insert(ShowingInsert.from(uid, input)) }
    }

    override suspend fun mine(): List<Showing> {
        val uid = user.require()
        return remote {
            client.from("showings").select(Columns.raw(Selects.SHOWING)) {
                filter { eq("created_by", uid) }
                order("starts_at", Order.ASCENDING)
            }.decodeList<Showing>()
        }
    }

    override suspend fun cancel(id: Long) {
        val uid = user.require()
        val rows = remote {
            client.from("showings").update(ShowingStatusPatch(ShowingStatus.CANCELLED)) {
                select(Columns.raw("id"))
                filter {
                    eq("id", id)
                    eq("created_by", uid)
                }
            }.decodeList<IdRow>()
        }
        // Without an UPDATE policy for visitors, RLS silently matches zero rows.
        if (rows.isEmpty()) {
            throw ReglaDataException("Não foi possível cancelar pelo app. Fale com o corretor pelo WhatsApp.")
        }
    }
}

// ─── leads (realtor) ──────────────────────────────────────────────────

internal class SupabaseLeadRepository(
    private val client: SupabaseClient,
    private val user: CurrentUser,
) : LeadRepository {

    override suspend fun list(stage: InquiryStage?): List<Inquiry> = remote {
        client.from("inquiries").select(Columns.raw(Selects.INQUIRY)) {
            if (stage != null) filter { eq("stage", wire(stage)) }
            order("last_activity_at", Order.DESCENDING)
            limit(200)
        }.decodeList<Inquiry>()
    }

    override suspend fun get(id: Long): Inquiry = remote {
        client.from("inquiries").select(Columns.raw(Selects.INQUIRY)) {
            filter { eq("id", id) }
        }.decodeSingleOrNull<Inquiry>()
    } ?: throw NoSuchElementException("Lead não encontrado")

    override suspend fun update(id: Long, stage: InquiryStage?, priority: Priority?, read: Boolean?) {
        if (stage == null && priority == null && read == null) return
        val patch = buildJsonObject {
            stage?.let { put("stage", wire(it)) }
            priority?.let { put("priority", wire(it)) }
            read?.let { put("read", it) }
        }
        val rows = remote {
            client.from("inquiries").update(patch) {
                select(Columns.raw("id"))
                filter { eq("id", id) }
            }.decodeList<IdRow>()
        }
        if (rows.isEmpty()) throw NoSuchElementException("Lead não encontrado")
    }

    override suspend fun notes(inquiryId: Long): List<LeadNote> = remote {
        client.from("lead_notes").select {
            filter { eq("inquiry_id", inquiryId) }
            order("created_at", Order.DESCENDING)
        }.decodeList<LeadNote>()
    }

    override suspend fun addNote(inquiryId: Long, body: String): LeadNote {
        require(body.isNotBlank()) { "Escreva a nota" }
        val uid = user.require()
        return remote {
            client.from("lead_notes").insert(NoteInsert(inquiryId, body.trim(), uid)) {
                select()
            }.decodeSingle<LeadNote>()
        }
    }
}

// ─── agenda (realtor) ─────────────────────────────────────────────────

internal class SupabaseAgendaRepository(private val client: SupabaseClient) : AgendaRepository {

    override suspend fun range(fromIso: String, toIso: String): List<Showing> = remote {
        client.from("showings").select(Columns.raw(Selects.SHOWING)) {
            // supabase-kt sends only the first value per query key, so a two-sided range on one
            // column must go through and=(…).
            filter {
                and {
                    gte("starts_at", fromIso)
                    lt("starts_at", toIso)
                }
            }
            order("starts_at", Order.ASCENDING)
        }.decodeList<Showing>()
    }

    override suspend fun setStatus(id: Long, status: ShowingStatus) {
        val rows = remote {
            client.from("showings").update(ShowingStatusPatch(status)) {
                select(Columns.raw("id"))
                filter { eq("id", id) }
            }.decodeList<IdRow>()
        }
        if (rows.isEmpty()) throw NoSuchElementException("Visita não encontrada")
    }
}

// ─── dashboard (realtor) ──────────────────────────────────────────────

internal class SupabaseDashboardRepository(
    private val client: SupabaseClient,
    private val today: () -> LocalDate = { LocalDate.now(BRT) },
) : DashboardRepository {

    private suspend fun countRows(table: String, filters: PostgrestFilterBuilder.() -> Unit): Int =
        client.from(table).select(Columns.raw("id")) {
            head = true
            count(Count.EXACT)
            filter(filters)
        }.countOrNull()?.toInt() ?: 0

    override suspend fun stats(): DashboardStats = remote {
        val day = today()
        val start = day.atStartOfDay(BRT).toOffsetDateTime().toString()
        val end = day.plusDays(1).atStartOfDay(BRT).toOffsetDateTime().toString()
        coroutineScope {
            val leads = async { countRows("inquiries") { eq("stage", wire(InquiryStage.INBOX)) } }
            val apps = async {
                countRows("applications") {
                    eq("awaiting", "realtor")
                }
            }
            val visits = async {
                countRows("showings") {
                    and {
                        gte("starts_at", start)
                        lt("starts_at", end)
                    }
                }
            }
            val live = async { countRows("listings") { eq("status", wire(ListingStatus.LIVE)) } }
            DashboardStats(
                newLeads = leads.await(),
                pendingApplications = apps.await(),
                visitsToday = visits.await(),
                liveListings = live.await(),
            )
        }
    }

    override fun changes(): Flow<Unit> = channelFlow {
        val rt = client.channel("regla-dashboard-${channelSeq.incrementAndGet()}")
        val flows = listOf("inquiries", "applications", "showings").map { t ->
            rt.postgresChangeFlow<PostgresAction.Insert>(schema = "public") { table = t }
        }
        flows.forEach { f -> launch(start = CoroutineStart.UNDISPATCHED) { f.collect { send(Unit) } } }
        try {
            rt.subscribe()
            awaitCancellation()
        } finally {
            withContext(NonCancellable) { runCatching { client.realtime.removeChannel(rt) } }
        }
    }

    private companion object {
        val channelSeq = AtomicInteger(0)
    }
}
