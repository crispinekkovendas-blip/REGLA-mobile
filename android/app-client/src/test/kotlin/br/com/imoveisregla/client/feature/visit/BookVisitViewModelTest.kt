package br.com.imoveisregla.client.feature.visit

import br.com.imoveisregla.core.data.fake.FakeBackend
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.ZonedDateTime

@OptIn(ExperimentalCoroutinesApi::class)
class BookVisitViewModelTest {
    private val fixedNow = ZonedDateTime.of(2026, 10, 5, 8, 0, 0, 0, SAO_PAULO) // Monday 08:00

    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @After fun tearDown() = Dispatchers.resetMain()

    private fun vm(fb: FakeBackend) = BookVisitViewModel(1, fb, clock = { fixedNow })

    @Test fun prefillsFromProfileAndListing() {
        val s = vm(FakeBackend(asRealtor = false)).state.value
        assertFalse(s.loading)
        assertEquals("Apartamento com varanda em Pinheiros", s.listing?.title)
        assertNotNull(s.photoUrl)
        assertEquals("Mariana Souza", s.name)
        assertEquals("cliente@exemplo.com", s.email)
        assertEquals("(11) 98765-4321", s.phone)
        assertEquals(LocalDate.of(2026, 10, 5), s.selectedDay)
        assertEquals(19, s.slots.size)
        assertEquals(14, s.days.size)
    }

    @Test fun validationErrorsBlockBooking() {
        val fb = FakeBackend(asRealtor = false)
        val before = fb.showingRows.size
        val vm = vm(fb)
        vm.onName("")
        vm.onEmail("nao-e-email")
        vm.onPhone("1199")
        vm.confirm()
        val s = vm.state.value
        assertEquals(setOf("visitorName", "visitorEmail", "visitorPhone", "startsAt"), s.errors.keys)
        assertFalse(s.done)
        assertEquals(before, fb.showingRows.size)
    }

    @Test fun disabledSlotCannotBeSelected() {
        val vm = BookVisitViewModel(1, FakeBackend(asRealtor = false), clock = { fixedNow.withHour(12) })
        val disabled = vm.state.value.slots.first { !it.enabled }
        vm.selectSlot(disabled)
        assertEquals(null, vm.state.value.selectedSlot)
    }

    @Test fun successfulBookingCreatesShowingForClient() {
        val fb = FakeBackend(asRealtor = false)
        val vm = vm(fb)
        vm.selectDay(LocalDate.of(2026, 10, 6))
        val slot = vm.state.value.slots.first { it.label == "14:30" }
        vm.selectSlot(slot)
        vm.onNotes("Posso levar meu cachorro?")
        vm.confirm()
        val s = vm.state.value
        assertTrue(s.errors.toString(), s.done)
        val row = fb.showingRows.single { it.startsAt == "2026-10-06T14:30-03:00" }
        assertEquals(1L, row.listingId)
        assertEquals("client-1", row.createdBy)
        assertEquals("Mariana Souza", row.visitorName)
        assertEquals("cliente@exemplo.com", row.visitorEmail)
        assertEquals("(11) 98765-4321", row.visitorPhone)
        assertEquals("Posso levar meu cachorro?", row.notes)
    }

    @Test fun signedOutShowsGate() {
        val s = vm(FakeBackend(asRealtor = false, signedIn = false)).state.value
        assertFalse(s.signedIn)
        assertFalse(s.loading)
    }

    @Test fun missingListingShowsError() {
        val s = BookVisitViewModel(9999, FakeBackend(asRealtor = false), clock = { fixedNow }).state.value
        assertNotNull(s.loadError)
    }
}
