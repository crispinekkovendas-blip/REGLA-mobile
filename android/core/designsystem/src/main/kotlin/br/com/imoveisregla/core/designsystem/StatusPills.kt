package br.com.imoveisregla.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import br.com.imoveisregla.core.model.Affordability
import br.com.imoveisregla.core.model.ApplicationStatus
import br.com.imoveisregla.core.model.InquiryStage
import br.com.imoveisregla.core.model.ListingStatus
import br.com.imoveisregla.core.model.Priority
import br.com.imoveisregla.core.model.ShowingStatus
import br.com.imoveisregla.core.model.label

/** Foreground (text/dot) + background pair used by every status pill. */
data class PillColors(val foreground: Color, val background: Color)

object PillPalette {
    val Ok = PillColors(Regla.OkInk, Regla.OkSoft)
    val Warn = PillColors(Regla.WarnInk, Regla.WarnSoft)
    val Danger = PillColors(Regla.DangerInk, Regla.DangerSoft)
    val Info = PillColors(Regla.InfoInk, Regla.InfoSoft)
    val Brand = PillColors(Regla.CoralInk, Regla.CoralSoft)
    val Navy = PillColors(Regla.Navy, Regla.NavySoft)
    val Neutral = PillColors(Regla.Muted, Regla.Line)
}

fun ApplicationStatus.pillColors(): PillColors = when (this) {
    ApplicationStatus.SUBMITTED -> PillPalette.Info
    ApplicationStatus.UNDER_REVIEW -> PillPalette.Warn
    ApplicationStatus.NEGOTIATING -> PillPalette.Navy
    ApplicationStatus.ACCEPTED -> PillPalette.Ok
    ApplicationStatus.DOCS_REVIEW -> PillPalette.Info
    ApplicationStatus.DOCS_REQUESTED -> PillPalette.Brand
    ApplicationStatus.APPROVED -> PillPalette.Ok
    ApplicationStatus.REJECTED -> PillPalette.Danger
    ApplicationStatus.WITHDRAWN -> PillPalette.Neutral
}

fun ShowingStatus.pillColors(): PillColors = when (this) {
    ShowingStatus.SCHEDULED -> PillPalette.Info
    ShowingStatus.CONFIRMED -> PillPalette.Ok
    ShowingStatus.ATTENDED -> PillPalette.Navy
    ShowingStatus.NO_SHOW -> PillPalette.Danger
    ShowingStatus.CANCELLED -> PillPalette.Neutral
}

fun ListingStatus.pillColors(): PillColors = when (this) {
    ListingStatus.DRAFT -> PillPalette.Neutral
    ListingStatus.LIVE -> PillPalette.Ok
    ListingStatus.SOLD -> PillPalette.Navy
    ListingStatus.WITHDRAWN -> PillPalette.Danger
}

fun InquiryStage.pillColors(): PillColors = when (this) {
    InquiryStage.INBOX -> PillPalette.Brand
    InquiryStage.QUALIFIED -> PillPalette.Info
    InquiryStage.SHOWING -> PillPalette.Warn
    InquiryStage.OFFER -> PillPalette.Navy
    InquiryStage.CLOSED_WON -> PillPalette.Ok
    InquiryStage.CLOSED_LOST -> PillPalette.Neutral
}

fun Priority.pillColors(): PillColors = when (this) {
    Priority.LOW -> PillPalette.Neutral
    Priority.MEDIUM -> PillPalette.Warn
    Priority.HIGH -> PillPalette.Danger
}

fun Affordability.pillColors(): PillColors = when (this) {
    Affordability.OK -> PillPalette.Ok
    Affordability.TIGHT -> PillPalette.Warn
    Affordability.OVER -> PillPalette.Danger
    Affordability.UNKNOWN -> PillPalette.Neutral
}

@Composable
private fun Pill(text: String, colors: PillColors, modifier: Modifier) =
    StatusPill(text = text, color = colors.foreground, background = colors.background, modifier = modifier)

@Composable
fun ApplicationStatusPill(status: ApplicationStatus, modifier: Modifier = Modifier) =
    Pill(status.label, status.pillColors(), modifier)

@Composable
fun ShowingStatusPill(status: ShowingStatus, modifier: Modifier = Modifier) =
    Pill(status.label, status.pillColors(), modifier)

@Composable
fun ListingStatusPill(status: ListingStatus, modifier: Modifier = Modifier) =
    Pill(status.label, status.pillColors(), modifier)

@Composable
fun StagePill(stage: InquiryStage, modifier: Modifier = Modifier) =
    Pill(stage.label, stage.pillColors(), modifier)

/** Compact priority marker with a directional glyph: "Alta" ↑, "Média" –, "Baixa" ↓. */
@Composable
fun PriorityBadge(priority: Priority, modifier: Modifier = Modifier) {
    val c = priority.pillColors()
    val icon = when (priority) {
        Priority.HIGH -> Icons.Rounded.KeyboardArrowUp
        Priority.MEDIUM -> Icons.Rounded.Remove
        Priority.LOW -> Icons.Rounded.KeyboardArrowDown
    }
    IconBadge(priority.label, icon, c, modifier)
}

/** Shows whether rent fits the client's income (30% / 40% rule). */
@Composable
fun AffordabilityBadge(affordability: Affordability, modifier: Modifier = Modifier) {
    val c = affordability.pillColors()
    val icon = when (affordability) {
        Affordability.OK -> Icons.Outlined.CheckCircle
        Affordability.TIGHT -> Icons.Outlined.WarningAmber
        Affordability.OVER -> Icons.Outlined.ErrorOutline
        Affordability.UNKNOWN -> Icons.AutoMirrored.Outlined.HelpOutline
    }
    IconBadge(affordability.label, icon, c, modifier)
}

@Composable
private fun IconBadge(text: String, icon: ImageVector, c: PillColors, modifier: Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(c.background)
            .padding(start = 6.dp, end = 10.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = c.foreground, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(4.dp))
        Text(text, color = c.foreground, style = MaterialTheme.typography.labelMedium, maxLines = 1)
    }
}
