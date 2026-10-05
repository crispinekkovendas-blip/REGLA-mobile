package br.com.imoveisregla.realtor.feature.leads

import br.com.imoveisregla.core.model.Inquiry
import br.com.imoveisregla.core.model.InquiryStage
import br.com.imoveisregla.core.model.Showing
import java.time.Duration
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

/** Number of leads in every stage (stages with no leads map to 0). */
fun stageCounts(leads: List<Inquiry>): Map<InquiryStage, Int> {
    val grouped = leads.groupingBy { it.stage }.eachCount()
    return InquiryStage.entries.associateWith { grouped[it] ?: 0 }
}

/** Stage filter (null = Todos) + case-insensitive search over name, e-mail and message. */
fun filterLeads(leads: List<Inquiry>, stage: InquiryStage?, query: String): List<Inquiry> {
    val q = query.trim()
    return leads.filter { lead ->
        (stage == null || lead.stage == stage) &&
            (q.isEmpty() || listOf(lead.name, lead.email, lead.message).any { it.contains(q, ignoreCase = true) })
    }
}

private val DATE_BR: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

private fun parseIso(iso: String): OffsetDateTime? =
    runCatching { OffsetDateTime.parse(iso) }.getOrNull()
        ?: runCatching { java.time.Instant.parse(iso).atOffset(java.time.ZoneOffset.UTC) }.getOrNull()

/**
 * Portuguese relative time: "agora", "há 5 min", "há 2 h", "há 1 dia", "há 3 dias";
 * 30+ days falls back to "dd/MM/yyyy". Unparseable input → "".
 */
fun relativeTime(iso: String, now: OffsetDateTime = OffsetDateTime.now()): String {
    val t = parseIso(iso) ?: return ""
    val d = Duration.between(t, now)
    val minutes = d.toMinutes()
    return when {
        minutes < 1 -> "agora"
        minutes < 60 -> "há $minutes min"
        minutes < 60 * 24 -> "há ${d.toHours()} h"
        d.toDays() < 30 -> d.toDays().let { if (it == 1L) "há 1 dia" else "há $it dias" }
        else -> t.format(DATE_BR)
    }
}

/** "Carlos Lima" → "CL", "ana" → "A", "" → "?". */
fun initials(name: String): String {
    val parts = name.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
    if (parts.isEmpty()) return "?"
    val first = parts.first().first().uppercaseChar()
    return if (parts.size == 1) "$first" else "$first${parts.last().first().uppercaseChar()}"
}

/** Leads have no phone column: borrow it from a showing booked with the same e-mail. */
fun phoneFromShowings(email: String, showings: List<Showing>): String? =
    showings.firstOrNull { s ->
        s.visitorEmail?.trim()?.equals(email.trim(), ignoreCase = true) == true && !s.visitorPhone.isNullOrBlank()
    }?.visitorPhone
