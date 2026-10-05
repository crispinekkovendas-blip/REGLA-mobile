package br.com.imoveisregla.realtor.feature.agenda

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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

class AgendaLogicTest {
    private val today = LocalDate.of(2026, 10, 4) // a Sunday

    private fun showing(id: Long, at: String, status: ShowingStatus = ShowingStatus.SCHEDULED) =
        Showing(id = id, listingId = 1, startsAt = at, status = status)

    @Test
    fun agendaWindow_isSevenDaysFromStartOfTodayInSaoPaulo() {
        val (from, to) = agendaWindow(today)
        assertEquals("2026-10-04T00:00:00-03:00", from)
        assertEquals("2026-10-11T00:00:00-03:00", to)
        assertEquals(7, agendaDays(today).size)
        assertEquals(LocalDate.of(2026, 10, 10), agendaDays(today).last())
    }

    @Test
    fun groupByDay_usesLocalDayAndSortsByTime() {
        val grouped = groupByDay(
            listOf(
                showing(1, "2026-10-05T15:00:00-03:00"),
                showing(2, "2026-10-05T09:30:00-03:00"),
                // 01:30 UTC on the 5th is 22:30 on the 4th in São Paulo
                showing(3, "2026-10-05T01:30:00Z"),
                showing(4, "2026-10-07T10:00-03:00"),
                showing(5, "not-a-date"),
            ),
            SAO_PAULO,
        )
        assertEquals(
            listOf(LocalDate.of(2026, 10, 4), LocalDate.of(2026, 10, 5), LocalDate.of(2026, 10, 7)),
            grouped.keys.toList(),
        )
        assertEquals(listOf(3L), grouped[LocalDate.of(2026, 10, 4)]!!.map { it.id })
        assertEquals(listOf(2L, 1L), grouped[LocalDate.of(2026, 10, 5)]!!.map { it.id })
        assertEquals(listOf(4L), grouped[LocalDate.of(2026, 10, 7)]!!.map { it.id })
    }

    @Test
    fun dayLabels() {
        assertEquals("Hoje", dayLabel(today, today))
        assertEquals("Amanhã", dayLabel(today.plusDays(1), today))
        assertEquals("qua 7 out", dayLabel(LocalDate.of(2026, 10, 7), today))
        assertEquals("qui 8 out", dayLabel(LocalDate.of(2026, 10, 8), today))
        assertEquals("sáb 10 out", dayLabel(LocalDate.of(2026, 10, 10), today))
        assertEquals("ter 1 dez", dayLabel(LocalDate.of(2026, 12, 1), today))
    }

    @Test
    fun timesAndActions() {
        val s = showing(1, "2026-10-05T15:00:00-03:00").copy(durationMinutes = 45)
        assertEquals("15:00", showingTime(s))
        assertEquals("15:00 – 15:45", showingTimeRange(s))
        assertEquals(listOf(ShowingStatus.CONFIRMED, ShowingStatus.CANCELLED), agendaActions(ShowingStatus.SCHEDULED))
        assertEquals(
            listOf(ShowingStatus.ATTENDED, ShowingStatus.NO_SHOW, ShowingStatus.CANCELLED),
            agendaActions(ShowingStatus.CONFIRMED),
        )
        assertTrue(agendaActions(ShowingStatus.ATTENDED).isEmpty())
        assertTrue(agendaActions(ShowingStatus.CANCELLED).isEmpty())
        assertEquals(listOf("Confirmar", "Cancelar"), agendaActions(ShowingStatus.SCHEDULED).map { actionLabel(it) })
        assertEquals(1, activeCount(listOf(s, s.copy(id = 2, status = ShowingStatus.CANCELLED))))
        assertEquals(0, activeCount(null))
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class AgendaViewModelTest {
    private val today = LocalDate.of(2026, 10, 4)
    private lateinit var fake: FakeBackend

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        fake = FakeBackend(asRealtor = true, seed = false)
        fun add(id: Long, at: String, status: ShowingStatus) {
            fake.showingRows += Showing(
                id = id, listingId = 1, startsAt = at, status = status,
                visitorName = "Visitante $id", visitorPhone = "(11) 91234-5678",
            )
        }
        add(1, "2026-10-04T15:00:00-03:00", ShowingStatus.SCHEDULED)
        add(2, "2026-10-05T10:00:00-03:00", ShowingStatus.CONFIRMED)
        add(3, "2026-10-11T10:00:00-03:00", ShowingStatus.SCHEDULED) // outside the 7-day window
        add(4, "2026-10-03T10:00:00-03:00", ShowingStatus.ATTENDED)  // yesterday
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun vm() = AgendaViewModel(fake.agenda, SAO_PAULO, today = { today })
    private fun row(id: Long) = fake.showingRows.first { it.id == id }

    @Test
    fun loadsNextSevenDaysGroupedByDay() {
        val s = vm().state.value
        assertFalse(s.loading)
        assertNull(s.error)
        assertEquals(today, s.selected)
        assertEquals(listOf(today, today.plusDays(1)), s.byDay.keys.toList())
        assertEquals(listOf(1L), s.selectedShowings.map { it.id })
        assertEquals(setOf(1L, 2L), s.all.map { it.id }.toSet())
    }

    @Test
    fun selectDayChangesList() {
        val vm = vm()
        vm.select(today.plusDays(1))
        assertEquals(listOf(2L), vm.state.value.selectedShowings.map { it.id })
        vm.select(today.plusDays(3))
        assertTrue(vm.state.value.selectedShowings.isEmpty())
    }

    @Test
    fun scheduledToConfirmedToAttended() {
        val vm = vm()
        assertNull(vm.setStatus(1, ShowingStatus.CONFIRMED))
        assertEquals(ShowingStatus.CONFIRMED, row(1).status)
        assertEquals(ShowingStatus.CONFIRMED, vm.state.value.selectedShowings.single().status)
        assertEquals("Visita confirmada", vm.state.value.message)

        assertNull(vm.setStatus(1, ShowingStatus.ATTENDED))
        assertEquals(ShowingStatus.ATTENDED, row(1).status)

        // terminal: no further transitions
        assertNotNull(vm.setStatus(1, ShowingStatus.CANCELLED))
        assertEquals(ShowingStatus.ATTENDED, row(1).status)
    }

    @Test
    fun invalidTransitionsAreBlocked() {
        val vm = vm()
        assertNotNull(vm.setStatus(1, ShowingStatus.ATTENDED)) // scheduled → attended not allowed
        assertNotNull(vm.setStatus(1, ShowingStatus.NO_SHOW))
        assertEquals(ShowingStatus.SCHEDULED, row(1).status)
        assertNotNull(vm.setStatus(4, ShowingStatus.CANCELLED)) // not in the window
        assertNotNull(vm.setStatus(999, ShowingStatus.CONFIRMED))
    }

    @Test
    fun confirmedCanBeNoShowOrCancelled() {
        val vm = vm()
        assertNull(vm.setStatus(2, ShowingStatus.NO_SHOW))
        assertEquals(ShowingStatus.NO_SHOW, row(2).status)

        assertNull(vm.setStatus(1, ShowingStatus.CANCELLED))
        assertEquals(ShowingStatus.CANCELLED, row(1).status)
        assertEquals("Visita cancelada", vm.state.value.message)
    }
}
