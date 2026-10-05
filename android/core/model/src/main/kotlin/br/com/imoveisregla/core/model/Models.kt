package br.com.imoveisregla.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Row types mirroring the REGLA Supabase schema (supabase/migrations 0001–0012).
// Column names are snake_case in Postgres; @SerialName maps them.

@Serializable
enum class ListingType {
    @SerialName("apartment") APARTMENT,
    @SerialName("house") HOUSE,
    @SerialName("commercial") COMMERCIAL,
    @SerialName("land") LAND,
}

@Serializable
enum class ListingStatus {
    @SerialName("draft") DRAFT,
    @SerialName("live") LIVE,
    @SerialName("sold") SOLD,
    @SerialName("withdrawn") WITHDRAWN,
}

@Serializable
enum class Currency { USD, EUR, GBP, BRL }

@Serializable
data class ListingPhoto(
    val id: Long,
    @SerialName("listing_id") val listingId: Long,
    @SerialName("storage_path") val storagePath: String,
    @SerialName("alt_text") val altText: String? = null,
    val position: Int = 0,
)

@Serializable
data class Listing(
    val id: Long,
    val slug: String,
    val title: String,
    val city: String,
    val country: String,
    val neighborhood: String,
    val type: ListingType,
    val price: Long,
    val currency: Currency,
    val beds: Int = 0,
    val baths: Int = 0,
    @SerialName("area_m2") val areaM2: Int,
    val tags: List<String> = emptyList(),
    val summary: String = "",
    val description: String = "",
    val status: ListingStatus = ListingStatus.LIVE,
    val featured: Boolean = false,
    @SerialName("agent_id") val agentId: Long? = null,
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("updated_at") val updatedAt: String = "",
    @SerialName("listing_photos") val photos: List<ListingPhoto> = emptyList(),
) {
    /** No rent/sale column exists; rentals are tagged "Aluguel" or priced under R$ 50k. */
    val isRental: Boolean
        get() = tags.any { it.equals("aluguel", true) || it.equals("rent", true) } ||
            (currency == Currency.BRL && price < 50_000)
}

/** Minimal listing projection embedded in other rows. */
@Serializable
data class ListingRef(
    val id: Long,
    val title: String,
    val neighborhood: String = "",
    val city: String = "",
    val price: Long = 0,
    val currency: Currency = Currency.BRL,
)

@Serializable
enum class InquiryStage {
    @SerialName("inbox") INBOX,
    @SerialName("qualified") QUALIFIED,
    @SerialName("showing") SHOWING,
    @SerialName("offer") OFFER,
    @SerialName("closed_won") CLOSED_WON,
    @SerialName("closed_lost") CLOSED_LOST,
}

@Serializable
enum class Priority {
    @SerialName("low") LOW,
    @SerialName("medium") MEDIUM,
    @SerialName("high") HIGH,
}

@Serializable
data class Inquiry(
    val id: Long,
    val name: String,
    val email: String,
    val intent: String? = null,
    val region: String? = null,
    val message: String,
    @SerialName("property_id") val propertyId: Long? = null,
    val read: Boolean = false,
    val stage: InquiryStage = InquiryStage.INBOX,
    val priority: Priority = Priority.MEDIUM,
    @SerialName("last_activity_at") val lastActivityAt: String = "",
    @SerialName("assigned_agent_id") val assignedAgentId: Long? = null,
    @SerialName("created_at") val createdAt: String = "",
    val listings: ListingRef? = null,
)

@Serializable
data class LeadNote(
    val id: Long,
    @SerialName("inquiry_id") val inquiryId: Long,
    val body: String,
    @SerialName("author_id") val authorId: String? = null,
    @SerialName("created_at") val createdAt: String = "",
)

@Serializable
enum class ShowingType {
    @SerialName("private") PRIVATE,
    @SerialName("open_house") OPEN_HOUSE,
}

@Serializable
enum class ShowingStatus {
    @SerialName("scheduled") SCHEDULED,
    @SerialName("confirmed") CONFIRMED,
    @SerialName("attended") ATTENDED,
    @SerialName("no_show") NO_SHOW,
    @SerialName("cancelled") CANCELLED,
}

@Serializable
data class Showing(
    val id: Long,
    @SerialName("listing_id") val listingId: Long,
    @SerialName("inquiry_id") val inquiryId: Long? = null,
    val type: ShowingType = ShowingType.PRIVATE,
    @SerialName("starts_at") val startsAt: String,           // ISO-8601 with offset
    @SerialName("duration_minutes") val durationMinutes: Int = 30,
    val status: ShowingStatus = ShowingStatus.SCHEDULED,
    @SerialName("visitor_name") val visitorName: String? = null,
    @SerialName("visitor_email") val visitorEmail: String? = null,
    @SerialName("visitor_phone") val visitorPhone: String? = null,
    val notes: String? = null,
    @SerialName("created_by") val createdBy: String? = null,
    val listings: ListingRef? = null,
)

// ─── Mobile additions (0012) ──────────────────────────────────────────

@Serializable
enum class EmploymentType {
    @SerialName("clt") CLT,
    @SerialName("autonomo") AUTONOMO,
    @SerialName("empresario") EMPRESARIO,
    @SerialName("servidor") SERVIDOR,
    @SerialName("aposentado") APOSENTADO,
    @SerialName("estudante") ESTUDANTE,
    @SerialName("outro") OUTRO,
}

@Serializable
data class ClientProfile(
    @SerialName("user_id") val userId: String,
    @SerialName("full_name") val fullName: String,
    val email: String,
    val phone: String,
    val cpf: String? = null,
    @SerialName("birth_date") val birthDate: String? = null,   // YYYY-MM-DD
    val occupation: String? = null,
    @SerialName("employment_type") val employmentType: EmploymentType? = null,
    @SerialName("monthly_income") val monthlyIncome: Long? = null,  // whole BRL
    val residents: Int = 1,
    @SerialName("has_pets") val hasPets: Boolean = false,
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("updated_at") val updatedAt: String = "",
)

@Serializable
enum class ApplicationIntent {
    @SerialName("rent") RENT,
    @SerialName("buy") BUY,
}

@Serializable
enum class GuaranteeType {
    @SerialName("seguro_fianca") SEGURO_FIANCA,
    @SerialName("fiador") FIADOR,
    @SerialName("caucao") CAUCAO,
    @SerialName("titulo_capitalizacao") TITULO_CAPITALIZACAO,
    @SerialName("sem_garantia") SEM_GARANTIA,
}

@Serializable
enum class ApplicationStatus {
    @SerialName("submitted") SUBMITTED,
    @SerialName("under_review") UNDER_REVIEW,
    @SerialName("docs_requested") DOCS_REQUESTED,
    @SerialName("approved") APPROVED,
    @SerialName("rejected") REJECTED,
    @SerialName("withdrawn") WITHDRAWN;

    val isOpen: Boolean get() = this == SUBMITTED || this == UNDER_REVIEW || this == DOCS_REQUESTED
}

/** A "proposta" — the client's formal offer/application on a listing. */
@Serializable
data class Application(
    val id: Long,
    @SerialName("listing_id") val listingId: Long,
    @SerialName("user_id") val userId: String,
    val intent: ApplicationIntent,
    @SerialName("offered_price") val offeredPrice: Long,
    @SerialName("guarantee_type") val guaranteeType: GuaranteeType? = null,
    @SerialName("move_in_date") val moveInDate: String? = null,
    val message: String? = null,
    val status: ApplicationStatus = ApplicationStatus.SUBMITTED,
    @SerialName("reviewer_note") val reviewerNote: String? = null,
    @SerialName("reviewed_by") val reviewedBy: String? = null,
    @SerialName("inquiry_id") val inquiryId: Long? = null,
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("updated_at") val updatedAt: String = "",
    val listings: ListingRef? = null,
    @SerialName("client_profiles") val profile: ClientProfile? = null,
)

@Serializable
enum class DocumentKind {
    @SerialName("rg_cnh") RG_CNH,
    @SerialName("cpf") CPF,
    @SerialName("comprovante_renda") COMPROVANTE_RENDA,
    @SerialName("comprovante_residencia") COMPROVANTE_RESIDENCIA,
    @SerialName("extrato_bancario") EXTRATO_BANCARIO,
    @SerialName("imposto_renda") IMPOSTO_RENDA,
    @SerialName("outro") OUTRO,
}

@Serializable
data class ClientDocument(
    val id: Long,
    @SerialName("user_id") val userId: String,
    @SerialName("application_id") val applicationId: Long? = null,
    val kind: DocumentKind,
    val filename: String,
    @SerialName("storage_path") val storagePath: String,   // bucket client-documents, `${userId}/...`
    @SerialName("mime_type") val mimeType: String,
    @SerialName("size_bytes") val sizeBytes: Long,
    @SerialName("created_at") val createdAt: String = "",
)

// ─── Inputs / queries ─────────────────────────────────────────────────

data class ListingFilters(
    val query: String = "",
    val city: String? = null,
    val type: ListingType? = null,
    val minBeds: Int? = null,
    val maxPrice: Long? = null,
)

data class ClientProfileInput(
    val fullName: String,
    val email: String,
    val phone: String,
    val cpf: String? = null,
    val birthDate: String? = null,
    val occupation: String? = null,
    val employmentType: EmploymentType? = null,
    val monthlyIncome: Long? = null,
    val residents: Int = 1,
    val hasPets: Boolean = false,
)

data class ApplicationInput(
    val listingId: Long,
    val intent: ApplicationIntent,
    val offeredPrice: Long,
    val guaranteeType: GuaranteeType? = null,
    val moveInDate: String? = null,
    val message: String? = null,
)

data class VisitInput(
    val listingId: Long,
    val startsAt: String,          // ISO-8601 with offset, e.g. 2026-10-06T14:30:00-03:00
    val visitorName: String,
    val visitorEmail: String,
    val visitorPhone: String? = null,
    val notes: String? = null,
)

data class UploadFile(
    val name: String,
    val mimeType: String,
    val bytes: ByteArray,
)

data class DashboardStats(
    val newLeads: Int = 0,
    val pendingApplications: Int = 0,
    val visitsToday: Int = 0,
    val liveListings: Int = 0,
)

data class UserSession(
    val userId: String,
    val email: String,
)
