package br.com.imoveisregla.client.feature.proposals

import br.com.imoveisregla.core.data.fake.FakeBackend
import br.com.imoveisregla.core.model.Application
import br.com.imoveisregla.core.model.ApplicationIntent
import br.com.imoveisregla.core.model.ApplicationStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProposalsViewModelTest {

    @Before fun setUp() { Dispatchers.setMain(UnconfinedTestDispatcher()) }
    @After fun tearDown() { Dispatchers.resetMain() }

    private fun vm(db: FakeBackend) = ProposalsViewModel(db.auth, db.applications)

    @Test fun `loads the seeded proposal`() {
        val s = vm(FakeBackend(asRealtor = false)).state.value
        assertTrue(s.signedIn)
        assertFalse(s.loading)
        assertEquals(listOf(301L, 302L), s.items.map { it.id })
        assertEquals(ApplicationStatus.UNDER_REVIEW, s.items.first().status)
        assertEquals(ApplicationStatus.NEGOTIATING, s.items.last().status)
    }

    @Test fun `signed out shows login state`() {
        val s = vm(FakeBackend(asRealtor = false, signedIn = false)).state.value
        assertFalse(s.signedIn)
        assertTrue(s.items.isEmpty())
    }

    @Test fun `withdraw cancels an open proposal after confirmation`() {
        val db = FakeBackend(asRealtor = false)
        val vm = vm(db)
        val app = vm.state.value.items.first { it.id == 301L }
        vm.askWithdraw(app)
        assertEquals(app, vm.state.value.confirmWithdraw)
        vm.confirmWithdraw()
        assertNull(vm.state.value.confirmWithdraw)
        assertEquals(ApplicationStatus.WITHDRAWN, db.applicationRows.single { it.id == 301L }.status)
        assertEquals(ApplicationStatus.WITHDRAWN, vm.state.value.items.first { it.id == 301L }.status)
        assertEquals("Proposta cancelada", vm.state.value.message)
    }

    @Test fun `withdraw is ignored for closed proposals`() {
        val db = FakeBackend(asRealtor = false)
        val approved = Application(
            id = 999, listingId = 2, userId = "client-1", intent = ApplicationIntent.RENT, offeredPrice = 3_200,
            status = ApplicationStatus.APPROVED, createdAt = "2026-09-01T10:00:00-03:00",
        )
        db.applicationRows += approved
        val vm = vm(db)
        vm.askWithdraw(approved)
        assertNull(vm.state.value.confirmWithdraw)
        vm.withdraw(approved)
        assertEquals(ApplicationStatus.APPROVED, db.applicationRows.single { it.id == 999L }.status)
        assertNull(vm.state.value.message)
    }

    @Test fun `refresh picks up new rows`() {
        val db = FakeBackend(asRealtor = false)
        val vm = vm(db)
        db.applicationRows += Application(
            id = 777, listingId = 3, userId = "client-1", intent = ApplicationIntent.RENT, offeredPrice = 9_000,
            createdAt = "2099-01-01T10:00:00-03:00",
        )
        vm.refresh()
        assertFalse(vm.state.value.refreshing)
        assertEquals(listOf(777L, 301L, 302L), vm.state.value.items.map { it.id })
    }

    @Test fun `timeline step mapping`() {
        fun states(s: ApplicationStatus) = proposalTimeline(s).map { it.state }
        fun labels(s: ApplicationStatus) = proposalTimeline(s).map { it.label }

        val d = TimelineState.DONE
        val p = TimelineState.PENDING
        assertEquals(listOf(TimelineState.CURRENT, p, p, p), states(ApplicationStatus.SUBMITTED))
        assertEquals(listOf(d, TimelineState.CURRENT, p, p), states(ApplicationStatus.NEGOTIATING))
        assertEquals(listOf(d, d, TimelineState.WARNING, p), states(ApplicationStatus.ACCEPTED))
        assertEquals(listOf(d, d, TimelineState.CURRENT, p), states(ApplicationStatus.DOCS_REVIEW))
        assertEquals(listOf(d, d, TimelineState.WARNING, p), states(ApplicationStatus.DOCS_REQUESTED))
        assertEquals(listOf(d, d, d, d), states(ApplicationStatus.APPROVED))
        assertEquals(listOf("Enviada", "Negociação", "Documentos", "Aprovada"), labels(ApplicationStatus.APPROVED))
        assertEquals(TimelineState.FAILED, states(ApplicationStatus.REJECTED).last())
        assertEquals("Recusada", labels(ApplicationStatus.REJECTED).last())
        assertEquals(listOf("Enviada", "Cancelada"), labels(ApplicationStatus.WITHDRAWN))
    }

    @Test fun `date formatting`() {
        assertEquals("04/10/2026", formatProposalDate("2026-10-04T18:02:40-03:00"))
        assertEquals("04/10/2026", formatProposalDate("2026-10-04T20:00:00.123456+00:00"))
        assertEquals("20/10/2026", formatProposalDate("2026-10-20"))
        assertEquals("", formatProposalDate(""))
    }
}
