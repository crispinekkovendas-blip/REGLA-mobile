package br.com.imoveisregla.client.feature.visits

import br.com.imoveisregla.client.feature.visit.SAO_PAULO
import br.com.imoveisregla.core.data.fake.FakeBackend
import br.com.imoveisregla.core.model.Showing
import br.com.imoveisregla.core.model.ShowingStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.ZonedDateTime

@OptIn(ExperimentalCoroutinesApi::class)
class VisitsViewModelTest {
    private val now: ZonedDateTime = ZonedDateTime.now(SAO_PAULO)

    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @After fun tearDown() = Dispatchers.resetMain()

    private fun iso(t: ZonedDateTime) = t.toOffsetDateTime().toString()

    private fun backend() = FakeBackend(asRealtor = false).apply {
        showingRows += Showing(
            id = 900, listingId = 2, startsAt = iso(now.minusDays(3)), status = ShowingStatus.ATTENDED,
            createdBy = "client-1", listings = null,
        )
        showingRows += Showing(
            id = 901, listingId = 3, startsAt = iso(now.plusDays(5)), status = ShowingStatus.CANCELLED,
            createdBy = "client-1",
        )
        showingRows += Showing(
            id = 902, listingId = 5, startsAt = iso(now.plusDays(1)), status = ShowingStatus.CONFIRMED,
            createdBy = "client-1",
        )
        // someone else's visit must never show up
        showingRows += Showing(id = 903, listingId = 5, startsAt = iso(now.plusDays(1)), createdBy = "client-2")
    }

    @Test fun splitsUpcomingAndPast() {
        val s = VisitsViewModel(backend()).state.value
        assertFalse(s.loading)
        assertTrue(s.signedIn)
        assertEquals(listOf(902L, 203L), s.upcoming.map { it.id })
        assertEquals(listOf(901L, 900L), s.past.map { it.id })
    }

    @Test fun cancelMovesVisitToPast() {
        val fb = backend()
        val vm = VisitsViewModel(fb)
        vm.cancel(203)
        val s = vm.state.value
        assertEquals(listOf(902L), s.upcoming.map { it.id })
        assertTrue(s.past.any { it.id == 203L && it.status == ShowingStatus.CANCELLED })
        assertEquals(ShowingStatus.CANCELLED, fb.showingRows.first { it.id == 203L }.status)
        assertEquals("Visita cancelada", s.message)
        vm.messageShown()
        assertEquals(null, vm.state.value.message)
    }

    @Test fun canCancelOnlyFutureOpenVisits() {
        val fb = backend()
        val byId = fb.showingRows.associateBy { it.id }
        assertTrue(canCancel(byId.getValue(203), now))
        assertTrue(canCancel(byId.getValue(902), now))
        assertFalse(canCancel(byId.getValue(900), now))
        assertFalse(canCancel(byId.getValue(901), now))
        assertFalse(canCancel(byId.getValue(203).copy(startsAt = iso(now.minusHours(1))), now))
    }

    @Test fun signedOutState() {
        val s = VisitsViewModel(FakeBackend(asRealtor = false, signedIn = false)).state.value
        assertFalse(s.signedIn)
        assertFalse(s.loading)
    }

    @Test fun reloadsAfterSignIn() = kotlinx.coroutines.test.runTest {
        val fb = FakeBackend(asRealtor = false, signedIn = false)
        val vm = VisitsViewModel(fb)
        fb.auth.signIn("cliente@exemplo.com", "123456")
        val s = vm.state.value
        assertTrue(s.signedIn)
        assertEquals(listOf(203L), s.upcoming.map { it.id })
    }
}
