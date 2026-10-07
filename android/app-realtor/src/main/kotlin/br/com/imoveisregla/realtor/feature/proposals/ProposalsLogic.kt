package br.com.imoveisregla.realtor.feature.proposals

import androidx.compose.ui.graphics.Color
import br.com.imoveisregla.core.designsystem.Regla
import br.com.imoveisregla.core.designsystem.pillColors
import br.com.imoveisregla.core.model.Affordability
import br.com.imoveisregla.core.model.Application
import br.com.imoveisregla.core.model.ApplicationStatus
import br.com.imoveisregla.core.model.ClientDocument
import br.com.imoveisregla.core.model.DocumentKind
import br.com.imoveisregla.core.model.Party
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

/** Filter chips in display order. */
enum class ProposalFilter(val label: String, private val predicate: (Application) -> Boolean) {
    ALL("Todas", { true }),
    YOUR_TURN("Sua vez", { it.isTurnOf(Party.REALTOR) }),
    NEGOTIATION("Negociação", { it.status.isNegotiation }),
    DOCUMENTS("Documentos", { it.status.isDocumentsPhase }),
    APPROVED("Aprovadas", { it.status == ApplicationStatus.APPROVED }),
    CLOSED("Encerradas", { it.status == ApplicationStatus.REJECTED || it.status == ApplicationStatus.WITHDRAWN });

    fun matches(app: Application): Boolean = predicate(app)
}

/** Count per filter chip. Every filter is present, zero included. */
fun statusCounts(apps: List<Application>): Map<ProposalFilter, Int> =
    ProposalFilter.entries.associateWith { f -> apps.count(f::matches) }

fun filterApplications(apps: List<Application>, filter: ProposalFilter): List<Application> =
    apps.filter(filter::matches)

/** What the realtor can do right now on a proposta. */
enum class RealtorAction { RESPOND_OFFER, REVIEW_DOCS, WAIT_CLIENT, CLOSED }

fun realtorAction(app: Application): RealtorAction = when {
    !app.status.isOpen -> RealtorAction.CLOSED
    app.awaiting != Party.REALTOR -> RealtorAction.WAIT_CLIENT
    app.status.isNegotiation -> RealtorAction.RESPOND_OFFER
    app.status == ApplicationStatus.DOCS_REVIEW -> RealtorAction.REVIEW_DOCS
    else -> RealtorAction.WAIT_CLIENT
}

/** One-line state headline, realtor's point of view. */
fun realtorHeadline(app: Application): String = when (app.status) {
    ApplicationStatus.SUBMITTED, ApplicationStatus.UNDER_REVIEW -> "Nova proposta: aceite, recuse ou contraproponha"
    ApplicationStatus.NEGOTIATING ->
        if (app.awaiting == Party.REALTOR) "O cliente respondeu com uma contraproposta" else "Aguardando resposta do cliente"
    ApplicationStatus.ACCEPTED -> "Aceita · aguardando documentos do cliente"
    ApplicationStatus.DOCS_REVIEW -> "Documentos recebidos: analise e aprove"
    ApplicationStatus.DOCS_REQUESTED -> "Aguardando correção dos documentos"
    ApplicationStatus.APPROVED -> "Aprovada · siga com o contrato"
    ApplicationStatus.REJECTED -> "Proposta recusada"
    ApplicationStatus.WITHDRAWN -> "Cancelada pelo cliente"
}

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

fun statusColors(status: ApplicationStatus): Pair<Color, Color> =
    status.pillColors().let { it.foreground to it.background }

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

/** Direct status moves the realtor may make from [current] (negotiation moves go through accept/counter). */
fun allowedReviewTargets(current: ApplicationStatus): Set<ApplicationStatus> = when {
    !current.isOpen -> emptySet()
    current == ApplicationStatus.SUBMITTED -> setOf(ApplicationStatus.UNDER_REVIEW, ApplicationStatus.REJECTED)
    current.isNegotiation -> setOf(ApplicationStatus.REJECTED)
    current.isDocumentsPhase -> setOf(ApplicationStatus.DOCS_REQUESTED, ApplicationStatus.APPROVED, ApplicationStatus.REJECTED)
    else -> emptySet()
}

fun noteRequired(target: ApplicationStatus): Boolean =
    target == ApplicationStatus.REJECTED || target == ApplicationStatus.DOCS_REQUESTED

/** Null when the review is allowed, else a user-facing error. */
fun validateReview(current: ApplicationStatus, target: ApplicationStatus, note: String?): String? = when {
    !current.isOpen -> "Esta proposta já foi finalizada"
    current == target -> "A proposta já está como \"${target.label}\""
    target !in allowedReviewTargets(current) -> "Ação indisponível nesta etapa"
    noteRequired(target) && note.isNullOrBlank() -> "Escreva uma observação para o cliente"
    else -> null
}

/** Null when a counter-offer of [price] is valid against the [current] price on the table. */
fun validateCounter(price: Long?, current: Long): String? = when {
    price == null || price <= 0 -> "Informe um valor"
    price == current -> "Informe um valor diferente da oferta atual"
    else -> null
}

fun reviewSuccessMessage(target: ApplicationStatus): String = when (target) {
    ApplicationStatus.UNDER_REVIEW -> "Proposta marcada como em análise"
    ApplicationStatus.DOCS_REQUESTED -> "Correção solicitada ao cliente"
    ApplicationStatus.APPROVED -> "Proposta aprovada"
    ApplicationStatus.REJECTED -> "Proposta recusada"
    else -> "Proposta atualizada"
}

data class TimelineStep(val label: String, val done: Boolean, val current: Boolean, val tone: Color = Regla.Navy)

/** Enviada → Negociação → Aceita → Documentos → decisão. */
fun timelineSteps(app: Application): List<TimelineStep> {
    val s = app.status
    val closedEarly = (s == ApplicationStatus.REJECTED || s == ApplicationStatus.WITHDRAWN) && app.agreedPrice == null
    val reachedAccept = app.agreedPrice != null || s.isDocumentsPhase || s == ApplicationStatus.APPROVED
    val steps = mutableListOf(
        TimelineStep("Enviada", done = true, current = s == ApplicationStatus.SUBMITTED || s == ApplicationStatus.UNDER_REVIEW),
        TimelineStep("Negociação", done = s != ApplicationStatus.SUBMITTED && s != ApplicationStatus.UNDER_REVIEW && !closedEarly || reachedAccept,
            current = s == ApplicationStatus.NEGOTIATING),
    )
    if (!closedEarly) {
        steps += TimelineStep("Aceita", done = reachedAccept, current = s == ApplicationStatus.ACCEPTED, tone = Regla.Ok)
        steps += TimelineStep(
            if (s == ApplicationStatus.DOCS_REQUESTED) "Correção" else "Documentos",
            done = s == ApplicationStatus.DOCS_REVIEW || s == ApplicationStatus.DOCS_REQUESTED || s == ApplicationStatus.APPROVED,
            current = s == ApplicationStatus.DOCS_REVIEW || s == ApplicationStatus.DOCS_REQUESTED,
            tone = if (s == ApplicationStatus.DOCS_REQUESTED) Regla.Coral else Regla.Navy,
        )
    }
    steps += when (s) {
        ApplicationStatus.APPROVED -> TimelineStep("Aprovada", done = true, current = true, tone = Regla.Ok)
        ApplicationStatus.REJECTED -> TimelineStep("Recusada", done = true, current = true, tone = Regla.Danger)
        ApplicationStatus.WITHDRAWN -> TimelineStep("Cancelada", done = true, current = true, tone = Regla.Muted)
        else -> TimelineStep("Contrato", done = false, current = false)
    }
    return steps
}

fun firstName(fullName: String?): String = fullName?.trim()?.substringBefore(' ')?.takeIf { it.isNotBlank() } ?: ""
