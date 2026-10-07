package br.com.imoveisregla.core.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SerializationTest {
    private val json = Json { ignoreUnknownKeys = true }

    private fun <T> roundTrip(serializer: KSerializer<T>, values: List<T>) {
        for (v in values) {
            val encoded = json.encodeToString(serializer, v)
            assertEquals(v, json.decodeFromString(serializer, encoded))
        }
    }

    @Test fun applicationStatus_serialNames() {
        assertEquals("\"under_review\"", json.encodeToString(ApplicationStatus.serializer(), ApplicationStatus.UNDER_REVIEW))
        assertEquals(ApplicationStatus.UNDER_REVIEW, json.decodeFromString(ApplicationStatus.serializer(), "\"under_review\""))
        assertEquals("\"docs_requested\"", json.encodeToString(ApplicationStatus.serializer(), ApplicationStatus.DOCS_REQUESTED))
    }

    @Test fun enums_serialNamesAreSnakeCaseLowercase() {
        assertEquals("\"closed_won\"", json.encodeToString(InquiryStage.serializer(), InquiryStage.CLOSED_WON))
        assertEquals("\"no_show\"", json.encodeToString(ShowingStatus.serializer(), ShowingStatus.NO_SHOW))
        assertEquals("\"open_house\"", json.encodeToString(ShowingType.serializer(), ShowingType.OPEN_HOUSE))
        assertEquals("\"seguro_fianca\"", json.encodeToString(GuaranteeType.serializer(), GuaranteeType.SEGURO_FIANCA))
        assertEquals("\"titulo_capitalizacao\"", json.encodeToString(GuaranteeType.serializer(), GuaranteeType.TITULO_CAPITALIZACAO))
        assertEquals("\"comprovante_renda\"", json.encodeToString(DocumentKind.serializer(), DocumentKind.COMPROVANTE_RENDA))
        assertEquals("\"rg_cnh\"", json.encodeToString(DocumentKind.serializer(), DocumentKind.RG_CNH))
        assertEquals("\"autonomo\"", json.encodeToString(EmploymentType.serializer(), EmploymentType.AUTONOMO))
        assertEquals("\"apartment\"", json.encodeToString(ListingType.serializer(), ListingType.APARTMENT))
        assertEquals("\"live\"", json.encodeToString(ListingStatus.serializer(), ListingStatus.LIVE))
        assertEquals("\"high\"", json.encodeToString(Priority.serializer(), Priority.HIGH))
        assertEquals("\"rent\"", json.encodeToString(ApplicationIntent.serializer(), ApplicationIntent.RENT))
        assertEquals("\"BRL\"", json.encodeToString(Currency.serializer(), Currency.BRL))
    }

    @Test fun allEnums_roundTrip() {
        roundTrip(ApplicationStatus.serializer(), ApplicationStatus.entries)
        roundTrip(InquiryStage.serializer(), InquiryStage.entries)
        roundTrip(ShowingStatus.serializer(), ShowingStatus.entries)
        roundTrip(ShowingType.serializer(), ShowingType.entries)
        roundTrip(GuaranteeType.serializer(), GuaranteeType.entries)
        roundTrip(DocumentKind.serializer(), DocumentKind.entries)
        roundTrip(EmploymentType.serializer(), EmploymentType.entries)
        roundTrip(ListingType.serializer(), ListingType.entries)
        roundTrip(ListingStatus.serializer(), ListingStatus.entries)
        roundTrip(Priority.serializer(), Priority.entries)
        roundTrip(ApplicationIntent.serializer(), ApplicationIntent.entries)
        roundTrip(Currency.serializer(), Currency.entries)
    }

    @Test fun allEnums_haveLabels() {
        ApplicationStatus.entries.forEach { assertTrue(it.label.isNotBlank()) }
        InquiryStage.entries.forEach { assertTrue(it.label.isNotBlank()) }
        ShowingStatus.entries.forEach { assertTrue(it.label.isNotBlank()) }
        GuaranteeType.entries.forEach { assertTrue(it.label.isNotBlank()) }
        DocumentKind.entries.forEach { assertTrue(it.label.isNotBlank()) }
        EmploymentType.entries.forEach { assertTrue(it.label.isNotBlank()) }
        ListingType.entries.forEach { assertTrue(it.label.isNotBlank()) }
        ListingStatus.entries.forEach { assertTrue(it.label.isNotBlank()) }
        Priority.entries.forEach { assertTrue(it.label.isNotBlank()) }
        assertEquals("Em análise", ApplicationStatus.UNDER_REVIEW.label)
    }

    @Test fun applicationStatus_isOpen() {
        assertTrue(ApplicationStatus.SUBMITTED.isOpen)
        assertTrue(ApplicationStatus.UNDER_REVIEW.isOpen)
        assertTrue(ApplicationStatus.DOCS_REQUESTED.isOpen)
        assertFalse(ApplicationStatus.APPROVED.isOpen)
        assertFalse(ApplicationStatus.REJECTED.isOpen)
        assertFalse(ApplicationStatus.WITHDRAWN.isOpen)
        assertTrue(ApplicationStatus.NEGOTIATING.isOpen && ApplicationStatus.NEGOTIATING.isNegotiation)
        assertTrue(ApplicationStatus.ACCEPTED.isOpen && ApplicationStatus.ACCEPTED.isDocumentsPhase)
        assertTrue(ApplicationStatus.DOCS_REVIEW.isDocumentsPhase && !ApplicationStatus.DOCS_REVIEW.isNegotiation)
        assertEquals("\"docs_review\"", json.encodeToString(ApplicationStatus.serializer(), ApplicationStatus.DOCS_REVIEW))
    }

    @Test fun application_decodesNegotiation() {
        val app = json.decodeFromString(
            Application.serializer(),
            """
            {"id": 7, "listing_id": 3, "user_id": "u1", "intent": "rent", "offered_price": 5000,
             "status": "negotiating", "awaiting": "client", "agreed_price": null,
             "application_offers": [
               {"id": 1, "application_id": 7, "author": "client", "price": 5000, "created_at": "2026-10-06T10:00:00-03:00"},
               {"id": 2, "application_id": 7, "author": "realtor", "price": 5500, "message": "Pede 5.500", "created_at": "2026-10-06T11:00:00-03:00"}
             ]}
            """.trimIndent(),
        )
        assertEquals(Party.CLIENT, app.awaiting)
        assertEquals(Party.REALTOR, app.latestOffer?.author)
        assertEquals(5500L, app.currentPrice)
        assertTrue(app.isTurnOf(Party.CLIENT))
        assertFalse(app.isTurnOf(Party.REALTOR))
        assertEquals(5200L, app.copy(agreedPrice = 5200).currentPrice)
    }

    @Test fun listing_decodesSupabaseRow() {
        val row = """
            {"id":42,"slug":"apto-pinheiros","title":"Apto Pinheiros","city":"São Paulo","country":"BR",
             "neighborhood":"Pinheiros","type":"apartment","price":4500,"currency":"BRL","beds":2,"baths":1,
             "area_m2":68,"tags":["Aluguel"],"status":"live","agent_id":3,"extra_column":true,
             "listing_photos":[{"id":1,"listing_id":42,"storage_path":"42/a.jpg","position":0}]}
        """.trimIndent()
        val l = json.decodeFromString(Listing.serializer(), row)
        assertEquals(42L, l.id)
        assertEquals(ListingType.APARTMENT, l.type)
        assertEquals(68, l.areaM2)
        assertEquals(3L, l.agentId)
        assertEquals("42/a.jpg", l.photos.single().storagePath)
        assertTrue(l.isRental)
        assertEquals(l, json.decodeFromString(Listing.serializer(), json.encodeToString(Listing.serializer(), l)))
        assertTrue(json.encodeToString(Listing.serializer(), l).contains("\"area_m2\":68"))
    }

    @Test fun application_roundTripWithEmbeds() {
        val a = Application(
            id = 9, listingId = 42, userId = "u-1", intent = ApplicationIntent.RENT, offeredPrice = 4_300,
            guaranteeType = GuaranteeType.FIADOR, status = ApplicationStatus.UNDER_REVIEW,
            listings = ListingRef(42, "Apto Pinheiros"),
            profile = ClientProfile(userId = "u-1", fullName = "Maria", email = "m@x.com", phone = "11932210855", employmentType = EmploymentType.CLT),
        )
        val s = json.encodeToString(Application.serializer(), a)
        assertTrue(s.contains("\"status\":\"under_review\""))
        assertTrue(s.contains("\"offered_price\":4300"))
        assertTrue(s.contains("\"client_profiles\":"))
        assertTrue(s.contains("\"employment_type\":\"clt\""))
        assertEquals(a, json.decodeFromString(Application.serializer(), s))
    }

    @Test fun showing_decodesDefaults() {
        val s = json.decodeFromString(
            Showing.serializer(),
            """{"id":1,"listing_id":2,"starts_at":"2026-10-06T14:30:00-03:00","status":"no_show"}""",
        )
        assertEquals(ShowingStatus.NO_SHOW, s.status)
        assertEquals(ShowingType.PRIVATE, s.type)
        assertEquals(30, s.durationMinutes)
        assertNull(s.inquiryId)
    }

    // ─── Listing.isRental ────────────────────────────────────────────────

    private fun listing(price: Long, currency: Currency = Currency.BRL, tags: List<String> = emptyList()) = Listing(
        id = 1, slug = "s", title = "t", city = "São Paulo", country = "BR", neighborhood = "Centro",
        type = ListingType.APARTMENT, price = price, currency = currency, areaM2 = 50, tags = tags,
    )

    @Test fun isRental_byTag() {
        assertTrue(listing(900_000, tags = listOf("Aluguel")).isRental)
        assertTrue(listing(900_000, tags = listOf("ALUGUEL")).isRental)
        assertTrue(listing(900_000, tags = listOf("rent")).isRental)
        assertTrue(listing(900_000, tags = listOf("Varanda", "RENT")).isRental)
        assertFalse(listing(900_000, tags = listOf("Varanda", "Venda")).isRental)
    }

    @Test fun isRental_byBrlPrice() {
        assertTrue(listing(4_500).isRental)
        assertTrue(listing(49_999).isRental)
        assertFalse(listing(50_000).isRental)
        assertFalse(listing(1_850_000).isRental)
    }

    @Test fun isRental_foreignCurrencyNotByPrice() {
        assertFalse(listing(3_000, Currency.USD).isRental)
        assertTrue(listing(3_000, Currency.USD, listOf("rent")).isRental)
    }
}
