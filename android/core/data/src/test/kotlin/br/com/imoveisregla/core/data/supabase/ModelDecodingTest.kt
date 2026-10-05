package br.com.imoveisregla.core.data.supabase

import br.com.imoveisregla.core.model.Application
import br.com.imoveisregla.core.model.ApplicationInput
import br.com.imoveisregla.core.model.ApplicationIntent
import br.com.imoveisregla.core.model.ApplicationStatus
import br.com.imoveisregla.core.model.ClientDocument
import br.com.imoveisregla.core.model.ClientProfile
import br.com.imoveisregla.core.model.ClientProfileInput
import br.com.imoveisregla.core.model.Currency
import br.com.imoveisregla.core.model.DocumentKind
import br.com.imoveisregla.core.model.EmploymentType
import br.com.imoveisregla.core.model.GuaranteeType
import br.com.imoveisregla.core.model.Inquiry
import br.com.imoveisregla.core.model.InquiryStage
import br.com.imoveisregla.core.model.LeadNote
import br.com.imoveisregla.core.model.Listing
import br.com.imoveisregla.core.model.ListingStatus
import br.com.imoveisregla.core.model.ListingType
import br.com.imoveisregla.core.model.Priority
import br.com.imoveisregla.core.model.Showing
import br.com.imoveisregla.core.model.ShowingStatus
import br.com.imoveisregla.core.model.ShowingType
import br.com.imoveisregla.core.model.VisitInput
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Decodes realistic PostgREST payloads with the same Json config the Supabase client uses. */
class ModelDecodingTest {

    @Test
    fun listingWithPhotosAndUnknownColumns() {
        val l = ReglaJson.decodeFromString<List<Listing>>(Fixtures.LISTINGS).single()
        assertEquals(12L, l.id)
        assertEquals("apto-pinheiros-72m2", l.slug)
        assertEquals(ListingType.APARTMENT, l.type)
        assertEquals(Currency.BRL, l.currency)
        assertEquals(ListingStatus.LIVE, l.status)
        assertEquals(72, l.areaM2)
        assertEquals(listOf("Aluguel", "Novo"), l.tags)
        assertEquals(2, l.photos.size)
        assertEquals("12/b.jpg", l.photos.first { it.position == 0 }.storagePath)
        assertNull(l.agentId)
        assertTrue(l.isRental)
    }

    @Test
    fun inquiryWithListingEmbed() {
        val i = ReglaJson.decodeFromString<List<Inquiry>>(Fixtures.INQUIRIES)
        assertEquals(InquiryStage.OFFER, i[0].stage)
        assertEquals(Priority.HIGH, i[0].priority)
        assertEquals(12L, i[0].propertyId)
        assertEquals("Apartamento com varanda em Pinheiros", i[0].listings?.title)
        assertEquals(4_800L, i[0].listings?.price)
        // listing deleted → property_id null, embed null; unknown score columns ignored
        assertNull(i[1].listings)
        assertEquals(InquiryStage.INBOX, i[1].stage)
        assertFalse(i[1].read)
    }

    @Test
    fun showingWithListingEmbed() {
        val s = ReglaJson.decodeFromString<List<Showing>>(Fixtures.SHOWINGS).single()
        assertEquals(ShowingType.PRIVATE, s.type)
        assertEquals(ShowingStatus.CONFIRMED, s.status)
        assertEquals("2026-10-06T17:30:00+00:00", s.startsAt)
        assertEquals(45, s.durationMinutes)
        assertEquals("Pinheiros", s.listings?.neighborhood)
        assertEquals("6b1c2d3e-0000-4000-8000-000000000001", s.createdBy)
    }

    @Test
    fun applicationForReviewWithEmbeds() {
        val a = ReglaJson.decodeFromString<List<Application>>(Fixtures.APPLICATIONS_REVIEW).single()
        assertEquals(ApplicationIntent.RENT, a.intent)
        assertEquals(ApplicationStatus.UNDER_REVIEW, a.status)
        assertEquals(GuaranteeType.SEGURO_FIANCA, a.guaranteeType)
        assertEquals("2026-11-01", a.moveInDate)
        assertEquals(88L, a.inquiryId)
        assertEquals("Pinheiros", a.listings?.neighborhood)
        val p = a.profile!!
        assertEquals("Mariana Souza", p.fullName)
        assertEquals(EmploymentType.CLT, p.employmentType)
        assertEquals(18_000L, p.monthlyIncome)
        assertEquals(2, p.residents)
        assertTrue(p.hasPets)
    }

    @Test
    fun profileDocumentAndNote() {
        val p = ReglaJson.decodeFromString<ClientProfile>(Fixtures.PROFILE_MINIMAL)
        assertNull(p.cpf)
        assertNull(p.employmentType)
        assertEquals(1, p.residents)

        val d = ReglaJson.decodeFromString<List<ClientDocument>>(Fixtures.DOCUMENTS).single()
        assertEquals(DocumentKind.COMPROVANTE_RENDA, d.kind)
        assertEquals("application/pdf", d.mimeType)
        assertEquals(245_112L, d.sizeBytes)
        assertNull(d.applicationId)

        val n = ReglaJson.decodeFromString<List<LeadNote>>(Fixtures.NOTES).single()
        assertEquals(88L, n.inquiryId)
        assertEquals("Ligar amanhã", n.body)
    }

    @Test
    fun nullAndUnknownEnumsFallBackToDefaults() {
        val json = """[{"id":1,"name":"A","email":"a@b.co","message":"oi","stage":"brand_new","priority":null,"read":null}]"""
        val i = ReglaJson.decodeFromString<List<Inquiry>>(json).single()
        assertEquals(InquiryStage.INBOX, i.stage)
        assertEquals(Priority.MEDIUM, i.priority)
        assertFalse(i.read)
    }

    @Test
    fun insertPayloadsAreSnakeCaseWithoutGeneratedColumns() {
        val app = ReglaJson.encodeToJsonElement(
            ApplicationInsert.from("u1", ApplicationInput(12, ApplicationIntent.BUY, 950_000, null, "", "  ")),
        ).jsonObject
        assertEquals(
            setOf("listing_id", "user_id", "intent", "offered_price", "guarantee_type", "move_in_date", "message"),
            app.keys,
        )
        assertEquals("buy", app["intent"]!!.jsonPrimitive.content)
        assertEquals(JsonNull, app["move_in_date"])
        assertEquals(JsonNull, app["message"])

        val visit = ReglaJson.encodeToJsonElement(
            ShowingInsert.from("u1", VisitInput(12, "2026-10-06T14:30:00-03:00", "Ana", "ana@x.com")),
        ).jsonObject
        assertEquals("private", visit["type"]!!.jsonPrimitive.content)
        assertEquals("scheduled", visit["status"]!!.jsonPrimitive.content)
        assertEquals("u1", visit["created_by"]!!.jsonPrimitive.content)
        assertFalse("id" in visit)

        val profile = ReglaJson.encodeToJsonElement(
            ProfileUpsert.from("u1", ClientProfileInput("Ana Lima", "ana@x.com", "11999998888", residents = 1)),
        ).jsonObject
        assertEquals("1", profile["residents"]!!.jsonPrimitive.content)
        assertEquals("false", profile["has_pets"]!!.jsonPrimitive.content)
        assertFalse("created_at" in profile)
        assertFalse("updated_at" in profile)

        assertEquals("comprovante_renda", wire(DocumentKind.COMPROVANTE_RENDA))
        assertEquals("closed_won", wire(InquiryStage.CLOSED_WON))
    }
}

internal object Fixtures {
    const val LISTINGS = """[{
      "id": 12, "slug": "apto-pinheiros-72m2", "title": "Apartamento com varanda em Pinheiros",
      "city": "São Paulo", "country": "Brazil", "neighborhood": "Pinheiros", "type": "apartment",
      "price": 4800, "currency": "BRL", "beds": 2, "baths": 2, "area_m2": 72, "area_ft2": null,
      "tags": ["Aluguel", "Novo"], "palette": "navy", "shape": "apartment",
      "summary": "Dois dormitórios com varanda.", "description": "Completo.", "status": "live",
      "featured": true, "created_at": "2026-09-01T12:00:00+00:00", "updated_at": "2026-09-20T12:00:00+00:00",
      "created_by": null, "agent_id": null,
      "listing_photos": [
        {"id": 2, "listing_id": 12, "storage_path": "12/a.jpg", "alt_text": "Sala", "position": 1},
        {"id": 1, "listing_id": 12, "storage_path": "12/b.jpg", "alt_text": null, "position": 0}
      ]
    }]"""

    const val INQUIRIES = """[
      {"id": 88, "name": "Mariana Souza", "email": "m@x.com", "intent": "rent", "region": null,
       "message": "Proposta #5 via app", "property_id": 12, "read": false, "stage": "offer",
       "priority": "high", "last_activity_at": "2026-10-03T10:00:00+00:00", "assigned_agent_id": null,
       "created_at": "2026-10-03T09:00:00+00:00",
       "listings": {"id": 12, "title": "Apartamento com varanda em Pinheiros", "neighborhood": "Pinheiros",
                    "city": "São Paulo", "price": 4800, "currency": "BRL"}},
      {"id": 89, "name": "Carlos", "email": "c@x.com", "intent": null, "region": "Zona Oeste",
       "message": "Olá", "property_id": null, "read": false, "stage": "inbox", "priority": "medium",
       "last_activity_at": "2026-10-03T08:00:00+00:00", "created_at": "2026-10-03T08:00:00+00:00",
       "score": 42, "listings": null}
    ]"""

    const val SHOWINGS = """[{
      "id": 7, "listing_id": 12, "inquiry_id": null, "type": "private", "starts_at": "2026-10-06T17:30:00+00:00",
      "duration_minutes": 45, "status": "confirmed", "visitor_name": "Ana", "visitor_email": "ana@x.com",
      "visitor_phone": null, "notes": null, "created_at": "2026-10-01T00:00:00+00:00",
      "updated_at": "2026-10-01T00:00:00+00:00", "created_by": "6b1c2d3e-0000-4000-8000-000000000001",
      "session_uuid": null,
      "listings": {"id": 12, "title": "Apartamento", "neighborhood": "Pinheiros", "city": "São Paulo", "price": 4800, "currency": "BRL"}
    }]"""

    const val PROFILE_FULL = """{
      "user_id": "6b1c2d3e-0000-4000-8000-000000000001", "full_name": "Mariana Souza", "email": "m@x.com",
      "phone": "(11) 98765-4321", "cpf": "529.982.247-25", "birth_date": "1992-04-18", "occupation": "Designer",
      "employment_type": "clt", "monthly_income": 18000, "residents": 2, "has_pets": true,
      "created_at": "2026-09-30T00:00:00+00:00", "updated_at": "2026-09-30T00:00:00+00:00"
    }"""

    const val PROFILE_MINIMAL = """{
      "user_id": "u1", "full_name": "Ana Lima", "email": "ana@x.com", "phone": "11999998888",
      "cpf": null, "birth_date": null, "occupation": null, "employment_type": null, "monthly_income": null,
      "residents": 1, "has_pets": false, "created_at": "2026-09-30T00:00:00+00:00", "updated_at": "2026-09-30T00:00:00+00:00"
    }"""

    const val APPLICATION_ROW = """{
      "id": 5, "listing_id": 12, "user_id": "6b1c2d3e-0000-4000-8000-000000000001", "intent": "rent",
      "offered_price": 4600, "guarantee_type": "seguro_fianca", "move_in_date": "2026-11-01",
      "message": "Podemos fechar?", "status": "under_review", "reviewer_note": null, "reviewed_by": null,
      "inquiry_id": 88, "created_at": "2026-10-03T09:00:00+00:00", "updated_at": "2026-10-03T09:00:00+00:00",
      "listings": {"id": 12, "title": "Apartamento com varanda em Pinheiros", "neighborhood": "Pinheiros",
                   "city": "São Paulo", "price": 4800, "currency": "BRL"}"""

    const val APPLICATIONS_REVIEW = "[$APPLICATION_ROW, \"client_profiles\": $PROFILE_FULL}]"
    const val APPLICATIONS = "[$APPLICATION_ROW}]"

    const val DOCUMENTS = """[{
      "id": 31, "user_id": "u1", "application_id": null, "kind": "comprovante_renda",
      "filename": "holerite setembro.pdf", "storage_path": "u1/comprovante_renda/1759500000000-holerite_setembro.pdf",
      "mime_type": "application/pdf", "size_bytes": 245112, "created_at": "2026-10-03T09:00:00+00:00"
    }]"""

    const val NOTES = """[{"id": 3, "inquiry_id": 88, "body": "Ligar amanhã", "author_id": "r1", "created_at": "2026-10-03T09:00:00+00:00"}]"""
}
