package br.com.imoveisregla.realtor.feature.proposals

import br.com.imoveisregla.core.model.Application
import br.com.imoveisregla.core.model.ApplicationIntent
import br.com.imoveisregla.core.model.ApplicationStatus
import br.com.imoveisregla.core.model.ClientDocument
import br.com.imoveisregla.core.model.DocumentKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.OffsetDateTime
import java.time.ZoneOffset

class ProposalsLogicTest {

    private fun app(id: Long, status: ApplicationStatus) = Application(
        id = id, listingId = 1, userId = "u$id", intent = ApplicationIntent.RENT, offeredPrice = 4_000, status = status,
    )

    @Test
    fun statusCounts_countsEveryFilterIncludingZeroAndTotal() {
        val apps = listOf(
            app(1, ApplicationStatus.SUBMITTED),
            app(2, ApplicationStatus.SUBMITTED),
            app(3, ApplicationStatus.UNDER_REVIEW),
            app(4, ApplicationStatus.APPROVED),
            app(5, ApplicationStatus.WITHDRAWN),
        )
        val counts = statusCounts(apps)
        assertEquals(PROPOSAL_FILTERS.size, counts.size)
        assertEquals(5, counts[null])
        assertEquals(2, counts[ApplicationStatus.SUBMITTED])
        assertEquals(1, counts[ApplicationStatus.UNDER_REVIEW])
        assertEquals(0, counts[ApplicationStatus.DOCS_REQUESTED])
        assertEquals(1, counts[ApplicationStatus.APPROVED])
        assertEquals(0, counts[ApplicationStatus.REJECTED])
        assertEquals(1, counts[ApplicationStatus.WITHDRAWN])
    }

    @Test
    fun filterApplications_nullReturnsAll() {
        val apps = listOf(app(1, ApplicationStatus.SUBMITTED), app(2, ApplicationStatus.APPROVED))
        assertEquals(2, filterApplications(apps, null).size)
        assertEquals(listOf(2L), filterApplications(apps, ApplicationStatus.APPROVED).map { it.id })
        assertEquals(emptyList<Application>(), filterApplications(apps, ApplicationStatus.REJECTED))
    }

    @Test
    fun filterLabels_arePortuguese() {
        assertEquals(
            listOf("Todas", "Enviada", "Em análise", "Documentos pendentes", "Aprovada", "Recusada", "Cancelada"),
            PROPOSAL_FILTERS.map { filterLabel(it) },
        )
    }

    @Test
    fun priceDiffPercent_andFormatting() {
        val below = priceDiffPercent(4_600, 4_800)!!
        assertEquals(-4.1667, below, 0.001)
        assertEquals("-4,2%", formatDiffPercent(below))
        assertEquals("+5,0%", formatDiffPercent(priceDiffPercent(5_040, 4_800)))
        assertEquals("0%", formatDiffPercent(priceDiffPercent(4_800, 4_800)))
        assertEquals("-10,0%", formatDiffPercent(priceDiffPercent(3_555_000, 3_950_000)))
        assertNull(priceDiffPercent(1_000, 0))
        assertEquals("", formatDiffPercent(null))
    }

    @Test
    fun rentIncomePercent_roundsAndHandlesUnknownIncome() {
        assertEquals(26, rentIncomePercent(4_600, 18_000))
        assertEquals(30, rentIncomePercent(3_000, 10_000))
        assertNull(rentIncomePercent(3_000, null))
        assertNull(rentIncomePercent(3_000, 0))
    }

    @Test
    fun validateReview_rules() {
        // closed proposals cannot be acted on
        assertNotNull(validateReview(ApplicationStatus.APPROVED, ApplicationStatus.REJECTED, "x"))
        assertNotNull(validateReview(ApplicationStatus.WITHDRAWN, ApplicationStatus.APPROVED, null))
        // note required for rejection / docs request
        assertEquals(
            "Escreva uma observação para o cliente",
            validateReview(ApplicationStatus.UNDER_REVIEW, ApplicationStatus.REJECTED, "  "),
        )
        assertNotNull(validateReview(ApplicationStatus.SUBMITTED, ApplicationStatus.DOCS_REQUESTED, null))
        assertNull(validateReview(ApplicationStatus.SUBMITTED, ApplicationStatus.DOCS_REQUESTED, "Envie o RG"))
        // approve / under review don't need a note
        assertNull(validateReview(ApplicationStatus.UNDER_REVIEW, ApplicationStatus.APPROVED, null))
        assertNull(validateReview(ApplicationStatus.SUBMITTED, ApplicationStatus.UNDER_REVIEW, null))
        // no-op / invalid targets
        assertNotNull(validateReview(ApplicationStatus.UNDER_REVIEW, ApplicationStatus.UNDER_REVIEW, null))
        assertNotNull(validateReview(ApplicationStatus.UNDER_REVIEW, ApplicationStatus.WITHDRAWN, "x"))
        assertNotNull(validateReview(ApplicationStatus.UNDER_REVIEW, ApplicationStatus.SUBMITTED, "x"))
    }

    @Test
    fun missingRequiredDocs_listsOnlyAbsentRequiredKinds() {
        fun doc(kind: DocumentKind) = ClientDocument(1, "u", null, kind, "a.pdf", "u/a.pdf", "application/pdf", 10)
        assertEquals(REQUIRED_DOCS, missingRequiredDocs(emptyList()))
        assertEquals(
            listOf(DocumentKind.COMPROVANTE_RESIDENCIA),
            missingRequiredDocs(listOf(doc(DocumentKind.RG_CNH), doc(DocumentKind.COMPROVANTE_RENDA), doc(DocumentKind.OUTRO))),
        )
    }

    @Test
    fun relativeTime_andDates() {
        val now = OffsetDateTime.of(2026, 10, 4, 12, 0, 0, 0, ZoneOffset.ofHours(-3))
        assertEquals("agora", relativeTime(now.toString(), now))
        assertEquals("há 5 min", relativeTime(now.minusMinutes(5).toString(), now))
        assertEquals("há 6 h", relativeTime(now.minusHours(6).toString(), now))
        assertEquals("ontem", relativeTime(now.minusHours(30).toString(), now))
        assertEquals("há 3 dias", relativeTime(now.minusDays(3).toString(), now))
        assertEquals("01/09/2026", relativeTime(now.minusDays(33).toString(), now))
        assertEquals("", relativeTime("", now))
        assertEquals("24/10/2026", formatDateBr("2026-10-24"))
        assertEquals("18/04/1992", formatDateBr("1992-04-18"))
    }

    @Test
    fun formatBytes_usesKbAndMb() {
        assertEquals("820 KB", formatBytes(840_000))
        assertEquals("1,2 MB", formatBytes(1_300_000))
        assertEquals("12 B", formatBytes(12))
    }

    @Test
    fun timeline_reflectsStatus() {
        val docs = timelineSteps(app(1, ApplicationStatus.DOCS_REQUESTED)).map { it.label }
        assertEquals(listOf("Enviada", "Em análise", "Documentos pendentes", "Decisão"), docs)
        val approved = timelineSteps(app(1, ApplicationStatus.APPROVED))
        assertEquals("Aprovada", approved.last().label)
        assertEquals(true, approved.all { it.done })
        val submitted = timelineSteps(app(1, ApplicationStatus.SUBMITTED))
        assertEquals(listOf(true, false, false), submitted.map { it.done })
    }
}
