package br.com.imoveisregla.realtor.feature.dashboard

import br.com.imoveisregla.core.data.SessionState
import br.com.imoveisregla.core.data.fake.FakeBackend
import br.com.imoveisregla.core.model.ApplicationInput
import br.com.imoveisregla.core.model.ApplicationIntent
import br.com.imoveisregla.core.model.ClientProfileInput
import br.com.imoveisregla.core.model.InquiryStage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.OffsetDateTime
import java.time.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {

    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @After fun tearDown() = Dispatchers.resetMain()

    @Test
    fun loadsStatsAndSectionsFromFakeBackend() = runTest {
        val fake = FakeBackend(asRealtor = true)
        val vm = DashboardViewModel(fake)
        advanceUntilIdle()

        val s = vm.state.value
        assertFalse(s.loading)
        assertNull(s.error)
        val stats = assertNotNullAndGet(s.stats)
        assertEquals(2, stats.newLeads)
        assertEquals(1, stats.pendingApplications)
        assertEquals(7, stats.liveListings)

        assertEquals(2, s.newLeads.size)
        assertTrue(s.newLeads.all { it.stage == InquiryStage.INBOX })
        assertEquals(2, s.recentProposals.size)
        assertEquals(301L, s.recentProposals.first().id) // realtor's turn first
        assertTrue(s.upcomingVisits.size <= 3)
        assertEquals("corretor@imoveisregla.com.br", s.email)
    }

    @Test
    fun refreshesWhenDashboardChangesEmit() = runTest {
        val fake = FakeBackend(asRealtor = true)
        val vm = DashboardViewModel(fake)
        advanceUntilIdle()
        assertEquals(1, vm.state.value.stats?.pendingApplications)

        // Fake uid is realtor-1: it needs a profile before it can submit a proposta.
        fake.profiles.save(ClientProfileInput(fullName = "Ana Teste", email = "ana@exemplo.com", phone = "(11) 99999-0000"))
        val listingId = fake.listings.search().first().id
        fake.applications.submit(ApplicationInput(listingId = listingId, intent = ApplicationIntent.RENT, offeredPrice = 3_000))
        advanceUntilIdle()

        val s = vm.state.value
        assertEquals(2, s.stats?.pendingApplications)
        assertEquals(3, s.recentProposals.size)
        assertTrue(s.recentProposals.any { it.profile?.fullName == "Ana Teste" })
    }

    @Test
    fun pullToRefreshReloadsAndClearsRefreshing() = runTest {
        val fake = FakeBackend(asRealtor = true)
        val vm = DashboardViewModel(fake)
        advanceUntilIdle()
        fake.listings.setStatus(fake.listings.search().first().id, br.com.imoveisregla.core.model.ListingStatus.SOLD)

        vm.refresh()
        advanceUntilIdle()

        assertFalse(vm.state.value.refreshing)
        assertEquals(6, vm.state.value.stats?.liveListings)
    }

    @Test
    fun signOutEndsSession() = runTest {
        val fake = FakeBackend(asRealtor = true)
        val vm = DashboardViewModel(fake)
        vm.signOut()
        advanceUntilIdle()
        assertEquals(SessionState.SignedOut, fake.auth.session.value)
    }

    @Test
    fun greetingAndDateFormatting() {
        assertEquals("Bom dia", greetingFor(8))
        assertEquals("Boa tarde", greetingFor(14))
        assertEquals("Boa noite", greetingFor(21))
        assertEquals("Boa noite", greetingFor(2))

        val sat = OffsetDateTime.of(2026, 10, 3, 9, 0, 0, 0, ZoneOffset.ofHours(-3))
        assertEquals("Sábado, 3 de outubro", dashboardDate(sat))
        assertEquals("Hoje · 15:00", visitWhen(sat.withHour(15).toString(), sat))
        assertEquals("Amanhã · 10:00", visitWhen(sat.plusDays(1).withHour(10).toString(), sat))
    }

    private fun <T> assertNotNullAndGet(v: T?): T { assertNotNull(v); return v!! }
}
