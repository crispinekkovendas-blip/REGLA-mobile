package br.com.imoveisregla.core.data

import br.com.imoveisregla.core.data.fake.FakeBackend
import br.com.imoveisregla.core.model.ApplicationInput
import br.com.imoveisregla.core.model.ApplicationIntent
import br.com.imoveisregla.core.model.ApplicationStatus
import br.com.imoveisregla.core.model.GuaranteeType
import br.com.imoveisregla.core.model.Party
import br.com.imoveisregla.core.model.InquiryStage
import br.com.imoveisregla.core.model.ListingFilters
import br.com.imoveisregla.core.model.ListingStatus
import br.com.imoveisregla.core.model.ListingType
import br.com.imoveisregla.core.model.Priority
import br.com.imoveisregla.core.model.Showing
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FakeBackendTest {

    private val rentInput = ApplicationInput(listingId = 2, intent = ApplicationIntent.RENT, offeredPrice = 3_100)

    @Test
    fun submitRequiresProfile() = runTest {
        val fb = FakeBackend(asRealtor = false)
        fb.profileRows.remove("client-1")
        val e = expectThrows<IllegalStateException> { fb.applications.submit(rentInput) }
        assertTrue(e.message!!.contains("cadastro"))
        assertTrue(fb.applicationRows.none { it.listingId == 2L })
    }

    @Test
    fun submitCreatesOfferInquiry() = runTest {
        val fb = FakeBackend(asRealtor = false)
        val app = fb.applications.submit(rentInput)
        assertEquals(ApplicationStatus.SUBMITTED, app.status)
        assertEquals("client-1", app.userId)
        val lead = fb.inquiryRows.single { it.id == app.inquiryId }
        assertEquals(InquiryStage.OFFER, lead.stage)
        assertEquals(Priority.HIGH, lead.priority)
        assertEquals(2L, lead.propertyId)
        assertEquals("Mariana Souza", lead.name)
        assertTrue(fb.applications.mine().any { it.id == app.id })
    }

    @Test
    fun withdrawOnlyOpen() = runTest {
        val fb = FakeBackend(asRealtor = false)
        fb.applications.withdraw(301)
        assertEquals(ApplicationStatus.WITHDRAWN, fb.applicationRows.single { it.id == 301L }.status)
        // already withdrawn → no longer open
        expectThrows<IllegalStateException> { fb.applications.withdraw(301) }

        val other = fb.applications.submit(rentInput)
        val i = fb.applicationRows.indexOfFirst { it.id == other.id }
        fb.applicationRows[i] = fb.applicationRows[i].copy(status = ApplicationStatus.APPROVED)
        expectThrows<IllegalStateException> { fb.applications.withdraw(other.id) }
    }

    @Test
    fun reviewApprovedClosesLead() = runTest {
        val fb = FakeBackend(asRealtor = true)
        fb.applications.review(301, ApplicationStatus.APPROVED, "Aprovado")
        val app = fb.applications.get(301)
        assertEquals(ApplicationStatus.APPROVED, app.status)
        assertEquals("Aprovado", app.reviewerNote)
        assertEquals("realtor-1", app.reviewedBy)
        assertEquals(InquiryStage.CLOSED_WON, fb.inquiryRows.single { it.id == 105L }.stage)
    }

    @Test
    fun submitOpensNegotiationAwaitingRealtor() = runTest {
        val fb = FakeBackend(asRealtor = false)
        val app = fb.applications.submit(rentInput)
        assertEquals(Party.REALTOR, app.awaiting)
        assertEquals(listOf(Party.CLIENT), app.offers.map { it.author })
        // not the client's turn yet: no counter / accept / documents
        expectThrows<IllegalStateException> { fb.applications.counter(app.id, 4000, null) }
        expectThrows<IllegalStateException> { fb.applications.accept(app.id) }
        expectThrows<IllegalStateException> { fb.applications.markDocsSent(app.id) }
    }

    @Test
    fun clientCountersThenItIsRealtorTurn() = runTest {
        val fb = FakeBackend(asRealtor = false)
        // seed 302: owner countered R$ 3.800, client's turn
        assertTrue(fb.applications.get(302).isTurnOf(Party.CLIENT))
        val o = fb.applications.counter(302, 3_700, " Meio termo? ")
        assertEquals(Party.CLIENT, o.author)
        assertEquals("Meio termo?", o.message)
        assertEquals(GuaranteeType.CAUCAO, o.guaranteeType) // inherited from the previous offer
        val app = fb.applications.get(302)
        assertEquals(ApplicationStatus.NEGOTIATING, app.status)
        assertEquals(Party.REALTOR, app.awaiting)
        assertEquals(3_700L, app.currentPrice)
        assertEquals(3, app.offers.size)
        expectThrows<IllegalStateException> { fb.applications.counter(302, 3_650, null) }
    }

    @Test
    fun clientAcceptsCounterThenSendsDocuments() = runTest {
        val fb = FakeBackend(asRealtor = false)
        val accepted = fb.applications.accept(302)
        assertEquals(ApplicationStatus.ACCEPTED, accepted.status)
        assertEquals(3_800L, accepted.agreedPrice)
        assertEquals(Party.CLIENT, accepted.awaiting)
        val sent = fb.applications.markDocsSent(302)
        assertEquals(ApplicationStatus.DOCS_REVIEW, sent.status)
        assertEquals(Party.REALTOR, sent.awaiting)
        expectThrows<IllegalStateException> { fb.applications.markDocsSent(302) }
    }

    @Test
    fun realtorAcceptsCountersAndDeclines() = runTest {
        val fb = FakeBackend(asRealtor = true)
        // 301: client's opening offer, realtor's turn
        val o = fb.applications.counter(301, 4_750, "Proprietário pede 4.750")
        assertEquals(Party.REALTOR, o.author)
        assertEquals(Party.CLIENT, fb.applications.get(301).awaiting)
        expectThrows<IllegalStateException> { fb.applications.accept(301) } // own offer / not our turn

        val declined = fb.applications.decline(301, " Imóvel alugado ")
        assertEquals(ApplicationStatus.REJECTED, declined.status)
        assertEquals("Imóvel alugado", declined.reviewerNote)
        assertEquals(null, declined.awaiting)
        expectThrows<IllegalStateException> { fb.applications.decline(301) }
    }

    @Test
    fun docsRequestedHandsTurnBackToClient() = runTest {
        val fb = FakeBackend(asRealtor = true)
        fb.applications.review(301, ApplicationStatus.DOCS_REQUESTED, "Falta holerite")
        assertEquals(Party.CLIENT, fb.applications.get(301).awaiting)
        assertEquals(0, fb.dashboard.stats().pendingApplications)
    }

    @Test
    fun clientDeclineWithdraws() = runTest {
        val fb = FakeBackend(asRealtor = false)
        assertEquals(ApplicationStatus.WITHDRAWN, fb.applications.decline(302).status)
    }

    @Test
    fun reviewRejectedKeepsLeadStage() = runTest {
        val fb = FakeBackend(asRealtor = true)
        fb.applications.review(301, ApplicationStatus.REJECTED, null)
        assertEquals(InquiryStage.OFFER, fb.inquiryRows.single { it.id == 105L }.stage)
    }

    @Test
    fun favoritesArePerUser() = runTest {
        val fb = FakeBackend(asRealtor = false)
        fb.favoriteRows += "someone-else" to 3L
        assertTrue(fb.favorites.ids().isEmpty())
        fb.favorites.set(1, true)
        fb.favorites.set(1, true)
        assertEquals(setOf(1L), fb.favorites.ids())
        fb.favorites.set(1, false)
        assertTrue(fb.favorites.ids().isEmpty())
        assertTrue(("someone-else" to 3L) in fb.favoriteRows)
    }

    @Test
    fun signedOutThrowsNotAuthenticated() = runTest {
        val fb = FakeBackend(asRealtor = false, signedIn = false)
        expectThrows<NotAuthenticatedException> { fb.favorites.ids() }
        expectThrows<NotAuthenticatedException> { fb.profiles.mine() }
        expectThrows<NotAuthenticatedException> { fb.applications.submit(rentInput) }
        fb.auth.signIn("cliente@exemplo.com", "123456")
        assertTrue(fb.auth.session.value is SessionState.SignedIn)
        assertTrue(fb.favorites.ids().isEmpty())
    }

    @Test
    fun searchFilters() = runTest {
        val fb = FakeBackend(asRealtor = false)
        val all = fb.listings.search()
        assertTrue(all.all { it.status == ListingStatus.LIVE })
        assertFalse(all.any { it.id == 8L }) // draft
        assertEquals(listOf(1L, 3L), all.take(2).map { it.id }) // featured first, newest first

        assertEquals(listOf(1L), fb.listings.search(ListingFilters(query = "pinheiros")).map { it.id })
        assertEquals(setOf(3L, 7L), fb.listings.search(ListingFilters(type = ListingType.HOUSE)).map { it.id }.toSet())
        assertEquals(setOf(3L, 4L, 7L), fb.listings.search(ListingFilters(minBeds = 3)).map { it.id }.toSet())
        assertEquals(setOf(1L, 2L, 5L), fb.listings.search(ListingFilters(maxPrice = 5_000)).map { it.id }.toSet())
        assertTrue(fb.listings.search(ListingFilters(city = "Rio de Janeiro")).isEmpty())
        assertEquals(
            listOf(5L),
            fb.listings.search(ListingFilters(query = "mariana", type = ListingType.APARTMENT, minBeds = 2, maxPrice = 4_000)).map { it.id },
        )
    }

    @Test
    fun agendaRangeIsHalfOpen() = runTest {
        val fb = FakeBackend(asRealtor = true, seed = false)
        fun s(id: Long, at: String) = Showing(id = id, listingId = 1, startsAt = at)
        fb.showingRows += listOf(
            s(1, "2026-10-06T23:59:00-03:00"),
            s(2, "2026-10-06T00:00:00-03:00"),
            s(3, "2026-10-07T00:00:00-03:00"),          // == to → excluded
            s(4, "2026-10-06T02:30:00Z"),               // 05/10 23:30 BRT → excluded
            s(5, "2026-10-06T12:00:00Z"),               // 06/10 09:00 BRT → included
        )
        val got = fb.agenda.range("2026-10-06T00:00:00-03:00", "2026-10-07T00:00:00-03:00")
        assertEquals(listOf(2L, 5L, 1L), got.map { it.id })
    }
}
