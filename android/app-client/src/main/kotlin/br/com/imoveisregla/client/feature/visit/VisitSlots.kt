package br.com.imoveisregla.client.feature.visit

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.TextStyle
import java.util.Locale

/** REGLA visits happen in São Paulo time. */
val SAO_PAULO: ZoneId = ZoneId.of("America/Sao_Paulo")

private val PT_BR: Locale = Locale.forLanguageTag("pt-BR")

private val OPEN: LocalTime = LocalTime.of(9, 0)
private val CLOSE_WEEKDAY: LocalTime = LocalTime.of(18, 0)
private val CLOSE_SATURDAY: LocalTime = LocalTime.of(13, 0)
private const val SLOT_MINUTES = 30L
private const val MIN_NOTICE_MINUTES = 60L

/** A bookable visit start time. [enabled] is false when it is less than 1h from now (or past). */
data class Slot(val start: ZonedDateTime, val enabled: Boolean) {
    /** ISO-8601 with offset, e.g. "2026-10-06T14:30-03:00". */
    val isoStart: String get() = start.toOffsetDateTime().toString()

    /** "14:30" */
    val label: String get() = "%02d:%02d".format(start.hour, start.minute)
}

/** The next [count] bookable days starting at [today] (inclusive), skipping Sundays. */
fun nextDays(today: LocalDate, count: Int = 14): List<LocalDate> =
    generateSequence(today) { it.plusDays(1) }
        .filter { it.dayOfWeek != DayOfWeek.SUNDAY }
        .take(count)
        .toList()

/** Slots for [day]: 09:00–18:00 every 30 min (Saturdays until 13:00), none on Sundays. */
fun slotsFor(day: LocalDate, now: ZonedDateTime, zone: ZoneId = SAO_PAULO): List<Slot> {
    val close = when (day.dayOfWeek) {
        DayOfWeek.SUNDAY -> return emptyList()
        DayOfWeek.SATURDAY -> CLOSE_SATURDAY
        else -> CLOSE_WEEKDAY
    }
    val earliest = now.plusMinutes(MIN_NOTICE_MINUTES)
    val out = mutableListOf<Slot>()
    var t = OPEN
    while (!t.isAfter(close)) {
        val start = ZonedDateTime.of(day, t, zone)
        out += Slot(start, enabled = !start.isBefore(earliest))
        t = t.plusMinutes(SLOT_MINUTES)
    }
    return out
}

private fun short(s: String): String = s.replace(".", "").lowercase(PT_BR).take(3)

/** "seg" / "sáb" */
fun weekdayShort(day: LocalDate): String = short(day.dayOfWeek.getDisplayName(TextStyle.SHORT, PT_BR))

/** "out" */
fun monthShort(day: LocalDate): String = short(day.month.getDisplayName(TextStyle.SHORT, PT_BR))

/** "seg 6 out" */
fun dayLabel(day: LocalDate): String = "${weekdayShort(day)} ${day.dayOfMonth} ${monthShort(day)}"

/** "2026-10-07T10:00-03:00" → "Qua, 7 out · 10:00" (in [zone]). Falls back to the raw string. */
fun visitWhenLabel(iso: String, zone: ZoneId = SAO_PAULO): String {
    val t = parseStart(iso, zone) ?: return iso
    val d = t.toLocalDate()
    val wd = weekdayShort(d).replaceFirstChar { it.titlecase(PT_BR) }
    return "$wd, ${d.dayOfMonth} ${monthShort(d)} · %02d:%02d".format(t.hour, t.minute)
}

fun parseStart(iso: String, zone: ZoneId = SAO_PAULO): ZonedDateTime? =
    runCatching { OffsetDateTime.parse(iso).atZoneSameInstant(zone) }.getOrNull()
