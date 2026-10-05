package br.com.imoveisregla.realtor.feature.agenda

import androidx.compose.ui.graphics.Color
import br.com.imoveisregla.core.designsystem.Regla
import br.com.imoveisregla.core.model.Showing
import br.com.imoveisregla.core.model.ShowingStatus
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

// Pure helpers for the realtor agenda (unit-tested).

val SAO_PAULO: ZoneId = ZoneId.of("America/Sao_Paulo")

const val AGENDA_DAYS = 7

private val ISO_OFFSET: DateTimeFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME
private val HM: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

private val WEEKDAYS = listOf("seg", "ter", "qua", "qui", "sex", "sáb", "dom")
private val MONTHS = listOf("jan", "fev", "mar", "abr", "mai", "jun", "jul", "ago", "set", "out", "nov", "dez")

/** [start of [today], start of [today] + [days]) as ISO-8601 with offset, e.g. 2026-10-04T00:00:00-03:00. */
fun agendaWindow(today: LocalDate, zone: ZoneId = SAO_PAULO, days: Int = AGENDA_DAYS): Pair<String, String> {
    val from = today.atStartOfDay(zone).toOffsetDateTime()
    val to = today.plusDays(days.toLong()).atStartOfDay(zone).toOffsetDateTime()
    return from.format(ISO_OFFSET) to to.format(ISO_OFFSET)
}

fun agendaDays(today: LocalDate, days: Int = AGENDA_DAYS): List<LocalDate> = (0 until days).map { today.plusDays(it.toLong()) }

private fun startOf(s: Showing): OffsetDateTime? = runCatching { OffsetDateTime.parse(s.startsAt) }.getOrNull()

/** Showings grouped by local day in [zone], days ascending and each day's showings by start time. */
fun groupByDay(showings: List<Showing>, zone: ZoneId = SAO_PAULO): Map<LocalDate, List<Showing>> =
    showings
        .mapNotNull { s -> startOf(s)?.let { it to s } }
        .sortedBy { it.first.toInstant() }
        .groupBy({ it.first.atZoneSameInstant(zone).toLocalDate() }, { it.second })
        .toSortedMap()

/** "Hoje", "Amanhã", otherwise e.g. "qua 7 out". */
fun dayLabel(date: LocalDate, today: LocalDate): String = when (date) {
    today -> "Hoje"
    today.plusDays(1) -> "Amanhã"
    else -> "${WEEKDAYS[date.dayOfWeek.value - 1]} ${date.dayOfMonth} ${MONTHS[date.monthValue - 1]}"
}

/** "qua" / "8" / "out" pieces for the compact day chip. */
fun weekdayShort(date: LocalDate): String = WEEKDAYS[date.dayOfWeek.value - 1]
fun monthShort(date: LocalDate): String = MONTHS[date.monthValue - 1]

/** "15:00". */
fun showingTime(s: Showing, zone: ZoneId = SAO_PAULO): String =
    startOf(s)?.atZoneSameInstant(zone)?.format(HM) ?: s.startsAt

/** "15:00 – 15:30". */
fun showingTimeRange(s: Showing, zone: ZoneId = SAO_PAULO): String {
    val start = startOf(s)?.atZoneSameInstant(zone) ?: return s.startsAt
    return start.format(HM) + " – " + start.plusMinutes(s.durationMinutes.toLong()).format(HM)
}

/** Allowed realtor transitions from each status. */
fun agendaActions(status: ShowingStatus): List<ShowingStatus> = when (status) {
    ShowingStatus.SCHEDULED -> listOf(ShowingStatus.CONFIRMED, ShowingStatus.CANCELLED)
    ShowingStatus.CONFIRMED -> listOf(ShowingStatus.ATTENDED, ShowingStatus.NO_SHOW, ShowingStatus.CANCELLED)
    else -> emptyList()
}

fun canTransition(from: ShowingStatus, to: ShowingStatus): Boolean = to in agendaActions(from)

fun actionLabel(target: ShowingStatus): String = when (target) {
    ShowingStatus.CONFIRMED -> "Confirmar"
    ShowingStatus.ATTENDED -> "Realizada"
    ShowingStatus.NO_SHOW -> "Não compareceu"
    ShowingStatus.CANCELLED -> "Cancelar"
    ShowingStatus.SCHEDULED -> "Reagendar"
}

fun actionSuccessMessage(target: ShowingStatus): String = when (target) {
    ShowingStatus.CONFIRMED -> "Visita confirmada"
    ShowingStatus.ATTENDED -> "Visita marcada como realizada"
    ShowingStatus.NO_SHOW -> "Visita marcada como não compareceu"
    ShowingStatus.CANCELLED -> "Visita cancelada"
    ShowingStatus.SCHEDULED -> "Visita atualizada"
}

fun showingStatusColors(status: ShowingStatus): Pair<Color, Color> = when (status) {
    ShowingStatus.SCHEDULED -> Regla.Info to Regla.InfoSoft
    ShowingStatus.CONFIRMED -> Regla.Ok to Regla.OkSoft
    ShowingStatus.ATTENDED -> Regla.Navy to Regla.NavySoft
    ShowingStatus.NO_SHOW -> Regla.Warn to Regla.WarnSoft
    ShowingStatus.CANCELLED -> Regla.Danger to Regla.DangerSoft
}

/** Count shown on a day chip: showings that still matter (cancelled ones excluded). */
fun activeCount(showings: List<Showing>?): Int = showings?.count { it.status != ShowingStatus.CANCELLED } ?: 0
