package br.com.imoveisregla.client.feature.visit

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZonedDateTime

class VisitSlotsTest {
    private val monday = LocalDate.of(2026, 10, 5)
    private val tuesday = LocalDate.of(2026, 10, 6)
    private val saturday = LocalDate.of(2026, 10, 10)
    private val sunday = LocalDate.of(2026, 10, 4)

    private fun at(day: LocalDate, h: Int, m: Int = 0) = ZonedDateTime.of(day.atTime(h, m), SAO_PAULO)
    private val earlier = at(LocalDate.of(2026, 10, 3), 12)

    @Test fun weekdayHas19SlotsFrom9To18() {
        val slots = slotsFor(monday, earlier)
        assertEquals(19, slots.size)
        assertEquals("09:00", slots.first().label)
        assertEquals("09:30", slots[1].label)
        assertEquals("18:00", slots.last().label)
        assertTrue(slots.all { it.enabled })
    }

    @Test fun saturdayEndsAt13() {
        val slots = slotsFor(saturday, earlier)
        assertEquals(9, slots.size)
        assertEquals("09:00", slots.first().label)
        assertEquals("13:00", slots.last().label)
    }

    @Test fun sundayHasNoSlotsAndIsSkipped() {
        assertEquals(DayOfWeek.SUNDAY, sunday.dayOfWeek)
        assertTrue(slotsFor(sunday, earlier).isEmpty())
        val days = nextDays(LocalDate.of(2026, 10, 3))
        assertEquals(14, days.size)
        assertTrue(days.none { it.dayOfWeek == DayOfWeek.SUNDAY })
        assertEquals(LocalDate.of(2026, 10, 3), days[0])
        assertEquals(monday, days[1])
        assertEquals(5, nextDays(monday, count = 5).size)
    }

    @Test fun slotsLessThanOneHourAwayAreDisabled() {
        val slots = slotsFor(monday, at(monday, 10, 10))
        val disabled = slots.filterNot { it.enabled }.map { it.label }
        assertEquals(listOf("09:00", "09:30", "10:00", "10:30", "11:00"), disabled)
        assertTrue(slots.first { it.label == "11:30" }.enabled)
    }

    @Test fun exactlyOneHourAwayIsEnabled() {
        val slots = slotsFor(monday, at(monday, 10, 0))
        assertTrue(slots.first { it.label == "11:00" }.enabled)
        assertFalse(slots.first { it.label == "10:30" }.enabled)
    }

    @Test fun pastDayIsFullyDisabled() {
        assertTrue(slotsFor(monday, at(tuesday, 8)).none { it.enabled })
    }

    @Test fun isoStartHasSaoPauloOffset() {
        val slot = slotsFor(tuesday, earlier).first { it.label == "14:30" }
        assertEquals("2026-10-06T14:30-03:00", slot.isoStart)
    }

    @Test fun portugueseLabels() {
        assertEquals("seg 5 out", dayLabel(monday))
        assertEquals("sáb 10 out", dayLabel(saturday))
        assertEquals("Qua, 7 out · 10:00", visitWhenLabel("2026-10-07T10:00-03:00"))
        assertEquals("Qua, 7 out · 10:00", visitWhenLabel("2026-10-07T13:00Z"))
    }
}
