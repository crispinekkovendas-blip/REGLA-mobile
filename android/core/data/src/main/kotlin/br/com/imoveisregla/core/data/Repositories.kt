package br.com.imoveisregla.core.data

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
import br.com.imoveisregla.core.model.ListingStatus
import br.com.imoveisregla.core.model.Priority
import br.com.imoveisregla.core.model.Showing
import br.com.imoveisregla.core.model.ShowingStatus
import br.com.imoveisregla.core.model.UploadFile
import br.com.imoveisregla.core.model.UserSession
import br.com.imoveisregla.core.model.VisitInput
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

// Data contract shared by both apps. All functions throw on failure
// (network / RLS errors surface as exceptions with a user-readable message).
// Methods that act "as the current user" throw NotAuthenticatedException when signed out.

class NotAuthenticatedException : IllegalStateException("Faça login para continuar")

sealed interface SessionState {
    data object Loading : SessionState
    data object SignedOut : SessionState
    data class SignedIn(val session: UserSession) : SessionState
}

interface AuthRepository {
    val session: StateFlow<SessionState>
    suspend fun signIn(email: String, password: String)
    suspend fun signUp(email: String, password: String)
    suspend fun signOut()
    /** True when the signed-in user is a REGLA realtor (`public.is_admin()`). */
    suspend fun isRealtor(): Boolean
}

interface ListingRepository {
    /** Live listings only (client app). */
    suspend fun search(filters: ListingFilters = ListingFilters()): List<Listing>
    suspend fun byIds(ids: List<Long>): List<Listing>
    suspend fun get(id: Long): Listing
    /** Every listing regardless of status (realtor app; requires is_admin). */
    suspend fun all(): List<Listing>
    suspend fun setStatus(id: Long, status: ListingStatus)
    /** Public URL of a photo in the listing-photos bucket. */
    fun photoUrl(storagePath: String): String
}

interface FavoriteRepository {
    suspend fun ids(): Set<Long>
    suspend fun set(listingId: Long, favorite: Boolean)
}

interface ProfileRepository {
    suspend fun mine(): ClientProfile?
    suspend fun save(input: ClientProfileInput): ClientProfile
}

/**
 * Propostas, QuintoAndar-style (migration 0014):
 * submit (cadastro + CPF + renda, no documents) → negotiation (accept / counter /
 * decline, whoever is `awaiting` acts) → ACCEPTED → client uploads documents and
 * calls [markDocsSent] → DOCS_REVIEW → realtor [review]s to APPROVED, or
 * DOCS_REQUESTED for corrections.
 *
 * Rows include `listings`, `offers` (oldest first) and, for the realtor, `profile`.
 * The caller's side is implied by who is signed in (realtor = is_admin).
 */
interface ApplicationRepository {
    // client
    suspend fun submit(input: ApplicationInput): Application
    suspend fun mine(): List<Application>
    suspend fun withdraw(id: Long)
    /** Client: documents uploaded for an ACCEPTED / DOCS_REQUESTED proposta. */
    suspend fun markDocsSent(id: Long): Application
    // both sides (turn-checked server-side)
    suspend fun get(id: Long): Application
    suspend fun counter(
        id: Long,
        price: Long,
        message: String?,
        guaranteeType: GuaranteeType? = null,
        moveInDate: String? = null,
    ): Offer
    /** Accept the other side's latest offer → ACCEPTED with agreedPrice. */
    suspend fun accept(id: Long): Application
    /** Realtor → REJECTED (with note); client → WITHDRAWN. */
    suspend fun decline(id: Long, note: String? = null): Application
    // realtor (requires is_admin)
    suspend fun forReview(status: ApplicationStatus? = null): List<Application>
    /** Direct status moves: UNDER_REVIEW, DOCS_REQUESTED, APPROVED, REJECTED. */
    suspend fun review(id: Long, status: ApplicationStatus, note: String?)
}

interface DocumentRepository {
    suspend fun upload(kind: DocumentKind, file: UploadFile, applicationId: Long? = null): ClientDocument
    /** Current user's documents, or [userId]'s when called by a realtor. */
    suspend fun list(userId: String? = null): List<ClientDocument>
    suspend fun delete(id: Long)
    /** Attach previously uploaded (unlinked) documents to an application. */
    suspend fun attachToApplication(documentIds: List<Long>, applicationId: Long)
    suspend fun signedUrl(storagePath: String, seconds: Int = 300): String
}

interface VisitRepository {
    suspend fun book(input: VisitInput)
    suspend fun mine(): List<Showing>
    suspend fun cancel(id: Long)
}

interface LeadRepository {
    suspend fun list(stage: InquiryStage? = null): List<Inquiry>
    suspend fun get(id: Long): Inquiry
    suspend fun update(id: Long, stage: InquiryStage? = null, priority: Priority? = null, read: Boolean? = null)
    suspend fun notes(inquiryId: Long): List<LeadNote>
    suspend fun addNote(inquiryId: Long, body: String): LeadNote
}

interface AgendaRepository {
    /** Showings with startsAt in [fromIso, toIso). */
    suspend fun range(fromIso: String, toIso: String): List<Showing>
    suspend fun setStatus(id: Long, status: ShowingStatus)
}

interface DashboardRepository {
    suspend fun stats(): DashboardStats
    /** Emits whenever a new inquiry / application / showing arrives (realtime). */
    fun changes(): Flow<Unit>
}
