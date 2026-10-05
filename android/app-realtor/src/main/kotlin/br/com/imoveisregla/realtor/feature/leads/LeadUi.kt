package br.com.imoveisregla.realtor.feature.leads

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import br.com.imoveisregla.core.designsystem.Regla
import br.com.imoveisregla.core.designsystem.StatusPill
import br.com.imoveisregla.core.model.Currency
import br.com.imoveisregla.core.model.InquiryStage
import br.com.imoveisregla.core.model.ListingRef
import br.com.imoveisregla.core.model.Priority
import br.com.imoveisregla.core.model.formatPrice
import br.com.imoveisregla.core.model.label

/** (foreground, background) for each pipeline stage. */
internal fun stageColors(stage: InquiryStage): Pair<Color, Color> = when (stage) {
    InquiryStage.INBOX -> Regla.Coral to Regla.CoralSoft
    InquiryStage.QUALIFIED -> Regla.Info to Regla.InfoSoft
    InquiryStage.SHOWING -> Regla.Warn to Regla.WarnSoft
    InquiryStage.OFFER -> Regla.Navy to Regla.NavySoft
    InquiryStage.CLOSED_WON -> Regla.Ok to Regla.OkSoft
    InquiryStage.CLOSED_LOST -> Regla.Danger to Regla.DangerSoft
}

internal fun priorityColors(p: Priority): Pair<Color, Color> = when (p) {
    Priority.HIGH -> Regla.Coral to Regla.CoralSoft
    Priority.MEDIUM -> Regla.Muted to Regla.NavySoft
    Priority.LOW -> Regla.Info to Regla.InfoSoft
}

@Composable
internal fun StagePill(stage: InquiryStage, modifier: Modifier = Modifier) {
    val (fg, bg) = stageColors(stage)
    StatusPill(stage.label, fg, bg, modifier)
}

@Composable
internal fun PriorityBadge(priority: Priority, modifier: Modifier = Modifier) {
    val (fg, bg) = priorityColors(priority)
    StatusPill(priority.label, fg, bg, modifier)
}

@Composable
internal fun InitialsAvatar(name: String, size: Dp = 56.dp, modifier: Modifier = Modifier) {
    Box(
        modifier.size(size).clip(CircleShape).background(Regla.Navy),
        contentAlignment = Alignment.Center,
    ) {
        Text(initials(name), color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
internal fun UnreadDot(unread: Boolean, modifier: Modifier = Modifier) {
    Box(modifier.size(10.dp).clip(CircleShape).background(if (unread) Regla.Coral else Color.Transparent))
}

/** ListingRef carries no tags, so rentals are guessed by price (same rule as Listing.isRental). */
internal fun refPrice(ref: ListingRef): String {
    val p = formatPrice(ref.price, ref.currency)
    return if (ref.currency == Currency.BRL && ref.price in 1 until 50_000) "$p/mês" else p
}
