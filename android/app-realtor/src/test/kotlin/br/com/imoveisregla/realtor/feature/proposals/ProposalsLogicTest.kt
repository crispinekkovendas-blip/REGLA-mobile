package br.com.imoveisregla.realtor.feature.proposals

import br.com.imoveisregla.core.model.Application
import br.com.imoveisregla.core.model.ApplicationIntent
import br.com.imoveisregla.core.model.ApplicationStatus
import br.com.imoveisregla.core.model.ClientDocument
import br.com.imoveisregla.core.model.DocumentKind
import br.com.imoveisregla.core.model.Party
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.OffsetDateTime
import java.time.ZoneOffset

class ProposalsLogicTest {

    private fun app(id: Long, status: ApplicationStatus, awaiting: Party? = null) = Application(
        id = id, listingId = 1, userId = "u$id", intent = ApplicationIntent.RENT, offeredPrice = 4_000, status = status,
        awaiting = awaiting,
    )

    @Test
    fun statusCounts_countsEveryFilterIncludingZero() {
        val apps = listOf(
            app(1, ApplicationStatus.SUBMITTED, Party.REALTOR),
            app(2, ApplicationStatus.NEGOTIATING, Party.CLIENT),
            app(3, ApplicationStatus.DOCS_REVIEW, Party.REALTOR),
            app(4, ApplicationStatus.APPROVED, null),
            app(5, ApplicationStatus.WITHDRAWN, null),
        )
        val counts = statusCounts(apps)
        assertEquals(ProposalFilter.entries.size, counts.size)
        assertEquals(5, counts[ProposalFilter.ALL])
        assertEquals(2, counts[ProposalFilter.YOUR_TURN])
        assertEquals(2, counts[ProposalFilter.NEGOTIATION])
        assertEquals(1, counts[ProposalFilter.DOCUMENTS])
        assertEquals(1, counts[ProposalFilter.APPROVED])
        assertEquals(1, counts[ProposalFilter.CLOSED])
    }

    @Test
    fun filterApplications_byFilter() {
        val apps = listOf(app(1, ApplicationStatus.SUBMITTED, Party.REALTOR), app(2, ApplicationStatus.APPROVED, null))
        assertEquals(2, filterApplications(apps, ProposalFilter.ALL).size)
        assertEquals(listOf(2L), filterApplications(apps, ProposalFilter.APPROVED).map { it.id })
        assertEquals(emptyList<Application>(), filterApplications(apps, ProposalFilter.CLOSED))
    }

    @Test
    fun filterLabels_arePortuguese() {
        assertEquals(
            listOf("Todas", "Sua vez", "Negociação", "Documentos", "Aprovadas", "Encerradas"),
            ProposalFilter.entries.map { it.label },
        )
    }

    @Test
    fun realtorAction_followsTurn() {
        assertEquals(RealtorAction.RESPOND_OFFER, realtorAction(app(1, ApplicationStatus.SUBMITTED, Party.REALTOR)))
        assertEquals(RealtorAction.RESPOND_OFFER, realtorAction(app(1, ApplicationStatus.NEGOTIATING, Party.REALTOR)))
        assertEquals(RealtorAction.WAIT_CLIENT, realtorAction(app(1, ApplicationStatus.NEGOTIATING, Party.CLIENT)))
        assertEquals(RealtorAction.WAIT_CLIENT, realtorAction(app(1, ApplicationStatus.ACCEPTED, Party.CLIENT)))
        assertEquals(RealtorAction.REVIEW_DOCS, realtorAction(app(1, ApplicationStatus.DOCS_REVIEW, Party.REALTOR)))
        assertEquals(RealtorAction.CLOSED, realtorAction(app(1, ApplicationStatus.REJECTED, null)))
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
        // negotiation phase: only "em análise" (from submitted) and rejection
        assertNull(validateReview(ApplicationStatus.SUBMITTED, ApplicationStatus.UNDER_REVIEW, null))
        assertEquals("Ação indisponível nesta etapa", validateReview(ApplicationStatus.UNDER_REVIEW, ApplicationStatus.APPROVED, null))
        assertEquals("Ação indisponível nesta etapa", validateReview(ApplicationStatus.NEGOTIATING, ApplicationStatus.DOCS_REQUESTED, "x"))
        assertEquals("Escreva uma observação para o cliente", validateReview(ApplicationStatus.UNDER_REVIEW, ApplicationStatus.REJECTED, "  "))
        assertNull(validateReview(ApplicationStatus.NEGOTIATING, ApplicationStatus.REJECTED, "Sem acordo"))
        // documents phase
        assertNull(validateReview(ApplicationStatus.DOCS_REVIEW, ApplicationStatus.APPROVED, null))
        assertNotNull(validateReview(ApplicationStatus.DOCS_REVIEW, ApplicationStatus.DOCS_REQUESTED, null))
        assertNull(validateReview(ApplicationStatus.DOCS_REVIEW, ApplicationStatus.DOCS_REQUESTED, "Envie o RG"))
        // no-op
        assertNotNull(validateReview(ApplicationStatus.DOCS_REQUESTED, ApplicationStatus.DOCS_REQUESTED, "x"))
    }

    @Test
    fun validateCounter_rules() {
        assertEquals("Informe um valor", validateCounter(null, 4_600))
        assertEquals("Informe um valor", validateCounter(0, 4_600))
        assertEquals("Informe um valor diferente da oferta atual", validateCounter(4_600, 4_600))
        assertNull(validateCounter(4_750, 4_600))
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
        val docs = timelineSteps(app(1, ApplicationStatus.DOCS_REQUESTED, Party.CLIENT)).map { it.label }
        assertEquals(listOf("Enviada", "Negociação", "Aceita", "Correção", "Contrato"), docs)
        val approved = timelineSteps(app(1, ApplicationStatus.APPROVED, null))
        assertEquals("Aprovada", approved.last().label)
        assertEquals(true, approved.all { it.done })
        val submitted = timelineSteps(app(1, ApplicationStatus.SUBMITTED, Party.REALTOR))
        assertEquals(listOf(true, false, false, false, false), submitted.map { it.done })
        val rejected = timelineSteps(app(1, ApplicationStatus.REJECTED, null)).map { it.label }
        assertEquals(listOf("Enviada", "Negociação", "Recusada"), rejected)
    }
}
