package br.com.imoveisregla.realtor.feature.proposals

import androidx.compose.ui.graphics.Color
import br.com.imoveisregla.core.designsystem.Regla
import br.com.imoveisregla.core.model.Affordability
import br.com.imoveisregla.core.model.Application
import br.com.imoveisregla.core.model.ApplicationStatus
import br.com.imoveisregla.core.model.ClientDocument
import br.com.imoveisregla.core.model.DocumentKind
import br.com.imoveisregla.core.model.label
import java.time.Duration
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

// Pure helpers for the realtor's proposal review screens (unit-tested).

internal val PT_BR: Locale = Locale.forLanguageTag("pt-BR")
internal val BRT: ZoneOffset = ZoneOffset.ofHours(-3)

/** Filter chips in display order; `null` = "Todas". */
val PROPOSAL_FILTERS: List<ApplicationStatus?> = listOf(
    null,
    ApplicationStatus.SUBMITTED,
    ApplicationStatus.UNDER_REVIEW,
    ApplicationStatus.DOCS_REQUESTED,
    ApplicationStatus.APPROVED,
    ApplicationStatus.REJECTED,
    ApplicationStatus.WITHDRAWN,
)

fun filterLabel(status: ApplicationStatus?): String = status?.label ?: "Todas"

/** Count per filter chip (key `null` = total). Every filter is present, zero included. */
fun statusCounts(apps: List<Application>): Map<ApplicationStatus?, Int> =
    PROPOSAL_FILTERS.associateWith { f -> if (f == null) apps.size else apps.count { it.status == f } }

fun filterApplications(apps: List<Application>, status: ApplicationStatus?): List<Application> =
    if (status == null) apps else apps.filter { it.status == status }

/** Percent difference of the offer vs the asking price (negative = below asking). Null when unknown. */
fun priceDiffPercent(offered: Long, listingPrice: Long): Double? =
    if (listingPrice <= 0) null else (offered - listingPrice) * 100.0 / listingPrice

/** -4.1666 → "-4,2%", 5.0 → "+5,0%", 0.0 → "0%", null → "". */
fun formatDiffPercent(pct: Double?): String {
    if (pct == null) return ""
    if (abs(pct) < 0.05) return "0%"
    val sign = if (pct < 0) "-" else "+"
    return sign + String.format(PT_BR, "%.1f", abs(pct)) + "%"
}

/** Rent as a share of monthly income, rounded: 4600/18000 → 26. Null when income unknown. */
fun rentIncomePercent(rent: Long, income: Long?): Int? =
    if (income == null || income <= 0) null else (rent * 100.0 / income).roundToInt()

fun statusColors(status: ApplicationStatus): Pair<Color, Color> = when (status) {
    ApplicationStatus.SUBMITTED -> Regla.Info to Regla.InfoSoft
    ApplicationStatus.UNDER_REVIEW -> Regla.Warn to Regla.WarnSoft
    ApplicationStatus.DOCS_REQUESTED -> Regla.Coral to Regla.CoralSoft
    ApplicationStatus.APPROVED -> Regla.Ok to Regla.OkSoft
    ApplicationStatus.REJECTED -> Regla.Danger to Regla.DangerSoft
    ApplicationStatus.WITHDRAWN -> Regla.Muted to Regla.NavySoft
}

fun affordabilityColor(a: Affordability): Color = when (a) {
    Affordability.OK -> Regla.Ok
    Affordability.TIGHT -> Regla.Warn
    Affordability.OVER -> Regla.Danger
    Affordability.UNKNOWN -> Regla.Muted
}

/** Realtor-facing wording (core labels speak to the client). */
fun affordabilityRealtorLabel(a: Affordability): String = when (a) {
    Affordability.OK -> "Cabe no orçamento"
    Affordability.TIGHT -> "Orçamento apertado"
    Affordability.OVER -> "Acima de 40% da renda"
    Affordability.UNKNOWN -> "Renda não informada"
}

private fun parseIso(iso: String?): OffsetDateTime? =
    iso?.takeIf { it.isNotBlank() }?.let { runCatching { OffsetDateTime.parse(it) }.getOrNull() }

/** "agora", "há 5 min", "há 6 h", "ontem", "há 3 dias", then dd/MM/yyyy. */
fun relativeTime(iso: String?, now: OffsetDateTime = OffsetDateTime.now(BRT)): String {
    val t = parseIso(iso) ?: return ""
    val d = Duration.between(t, now)
    val mins = d.toMinutes()
    return when {
        mins < 1 -> "agora"
        mins < 60 -> "há $mins min"
        mins < 24 * 60 -> "há ${d.toHours()} h"
        d.toDays() < 2 -> "ontem"
        d.toDays() < 7 -> "há ${d.toDays()} dias"
        else -> t.atZoneSameInstant(BRT).toLocalDate().format(DMY)
    }
}

private val DMY: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
private val DMY_HM: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm")

/** "2026-10-24" (or a full ISO timestamp) → "24/10/2026". Returns the input if unparseable. */
fun formatDateBr(iso: String?): String {
    if (iso.isNullOrBlank()) return ""
    return runCatching { LocalDate.parse(iso.take(10)).format(DMY) }.getOrDefault(iso)
}

fun formatDateTimeBr(iso: String?): String =
    parseIso(iso)?.atZoneSameInstant(BRT)?.format(DMY_HM) ?: formatDateBr(iso)

/** 840_000 → "820 KB", 1_300_000 → "1,2 MB". */
fun formatBytes(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "${(bytes / 1024.0).roundToInt()} KB"
    else -> String.format(PT_BR, "%.1f MB", bytes / (1024.0 * 1024.0))
}

val REQUIRED_DOCS: List<DocumentKind> =
    listOf(DocumentKind.RG_CNH, DocumentKind.COMPROVANTE_RENDA, DocumentKind.COMPROVANTE_RESIDENCIA)

fun missingRequiredDocs(docs: List<ClientDocument>): List<DocumentKind> {
    val have = docs.map { it.kind }.toSet()
    return REQUIRED_DOCS.filter { it !in have }
}

/** Statuses the realtor can move a proposal to from the review screen. */
val REVIEW_TARGETS: Set<ApplicationStatus> = setOf(
    ApplicationStatus.UNDER_REVIEW,
    ApplicationStatus.DOCS_REQUESTED,
    ApplicationStatus.APPROVED,
    ApplicationStatus.REJECTED,
)

fun noteRequired(target: ApplicationStatus): Boolean =
    target == ApplicationStatus.REJECTED || target == ApplicationStatus.DOCS_REQUESTED

/** Null when the review is allowed, else a user-facing error. */
fun validateReview(current: ApplicationStatus, target: ApplicationStatus, note: String?): String? = when {
    !current.isOpen -> "Esta proposta já foi finalizada"
    target !in REVIEW_TARGETS -> "Ação inválida"
    current == target -> "A proposta já está como \"${target.label}\""
    noteRequired(target) && note.isNullOrBlank() -> "Escreva uma observação para o cliente"
    else -> null
}

fun reviewSuccessMessage(target: ApplicationStatus): String = when (target) {
    ApplicationStatus.UNDER_REVIEW -> "Proposta marcada como em análise"
    ApplicationStatus.DOCS_REQUESTED -> "Documentos solicitados ao cliente"
    ApplicationStatus.APPROVED -> "Proposta aprovada"
    ApplicationStatus.REJECTED -> "Proposta recusada"
    else -> "Proposta atualizada"
}

data class TimelineStep(val label: String, val done: Boolean, val current: Boolean, val tone: Color = Regla.Navy)

/** Enviada → Em análise → (Documentos pendentes) → decisão. */
fun timelineSteps(app: Application): List<TimelineStep> {
    val s = app.status
    val steps = mutableListOf<TimelineStep>()
    steps += TimelineStep("Enviada", done = true, current = s == ApplicationStatus.SUBMITTED)
    val reviewReached = s != ApplicationStatus.SUBMITTED && s != ApplicationStatus.WITHDRAWN ||
        (s == ApplicationStatus.WITHDRAWN && app.reviewedBy != null)
    steps += TimelineStep("Em análise", done = reviewReached, current = s == ApplicationStatus.UNDER_REVIEW)
    if (s == ApplicationStatus.DOCS_REQUESTED) {
        steps += TimelineStep("Documentos pendentes", done = true, current = true, tone = Regla.Coral)
    }
    steps += when (s) {
        ApplicationStatus.APPROVED -> TimelineStep("Aprovada", done = true, current = true, tone = Regla.Ok)
        ApplicationStatus.REJECTED -> TimelineStep("Recusada", done = true, current = true, tone = Regla.Danger)
        ApplicationStatus.WITHDRAWN -> TimelineStep("Cancelada", done = true, current = true, tone = Regla.Muted)
        else -> TimelineStep("Decisão", done = false, current = false)
    }
    return steps
}

fun firstName(fullName: String?): String = fullName?.trim()?.substringBefore(' ')?.takeIf { it.isNotBlank() } ?: ""
