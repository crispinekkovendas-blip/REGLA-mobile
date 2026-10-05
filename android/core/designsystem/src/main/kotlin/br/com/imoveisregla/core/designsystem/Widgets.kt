package br.com.imoveisregla.core.designsystem

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.imoveisregla.core.model.Currency
import br.com.imoveisregla.core.model.digitsOnly
import br.com.imoveisregla.core.model.formatPrice

// ─── StatTile ─────────────────────────────────────────────────────────

/** Dashboard KPI tile: big number, small label, accent bar on top. */
@Composable
fun StatTile(label: String, value: String, modifier: Modifier = Modifier, accent: Color = Regla.Coral) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(Regla.RadiusCard),
        color = Regla.Surface,
        border = BorderStroke(1.dp, Regla.Line),
    ) {
        Column(Modifier.padding(16.dp)) {
            Box(
                Modifier
                    .width(24.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(accent),
            )
            Spacer(Modifier.height(12.dp))
            Text(
                value,
                style = MaterialTheme.typography.headlineLarge,
                color = Regla.Navy,
                maxLines = 1,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = Regla.Muted,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

// ─── Chips ────────────────────────────────────────────────────────────

/** Read-only row of tags (e.g. listing.tags). Scrolls horizontally when it overflows. */
@Composable
fun ChipRow(labels: List<String>, modifier: Modifier = Modifier) {
    Row(
        modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        labels.forEach { l ->
            Text(
                l,
                style = MaterialTheme.typography.labelMedium,
                color = Regla.Navy,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Regla.NavySoft)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            )
        }
    }
}

/** Single-select chip used by [FilterChipGroup]. */
@Composable
fun ReglaChip(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        modifier = modifier.semantics {
            this.selected = selected
            role = Role.Tab
        },
        shape = RoundedCornerShape(Regla.RadiusPill),
        color = if (selected) Regla.Navy else Regla.Surface,
        contentColor = if (selected) Color.White else Regla.Ink,
        border = if (selected) null else BorderStroke(1.dp, Regla.Line),
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (selected) {
                Icon(Icons.Rounded.Check, contentDescription = null, tint = Regla.Coral, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
            }
            Text(text, style = MaterialTheme.typography.labelLarge.copy(fontSize = 14.sp), maxLines = 1)
        }
    }
}

/**
 * Horizontally scrolling single-select chip group.
 * [selected] may be null (nothing selected); callers decide whether tapping the selected chip clears it.
 */
@Composable
fun <T> FilterChipGroup(
    options: List<T>,
    selected: T?,
    label: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { o ->
            ReglaChip(text = label(o), selected = o == selected, onClick = { onSelect(o) })
        }
    }
}

// ─── InfoRow ──────────────────────────────────────────────────────────

/** Label on the left (muted), value on the right (bold). */
@Composable
fun InfoRow(label: String, value: String, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = Regla.Muted, modifier = Modifier.weight(1f))
        Spacer(Modifier.width(12.dp))
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = Regla.Ink,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1.4f),
        )
    }
}

// ─── Avatar ───────────────────────────────────────────────────────────

/** "Ana Paula Souza" → "AS"; "maria" → "M"; blank → "?". */
fun initials(name: String): String {
    val parts = name.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
    return when {
        parts.isEmpty() -> "?"
        parts.size == 1 -> parts[0].take(1).uppercase()
        else -> (parts.first().take(1) + parts.last().take(1)).uppercase()
    }
}

private val AvatarPalette = listOf(
    Regla.Navy to Color.White,
    Regla.Coral to Color.White,
    Regla.InfoSoft to Regla.InfoInk,
    Regla.OkSoft to Regla.OkInk,
    Regla.CoralSoft to Regla.CoralInk,
    Regla.NavyMid to Color.White,
)

@Composable
fun Avatar(name: String, modifier: Modifier = Modifier, size: Dp = 40.dp) {
    val (bg, fg) = AvatarPalette[Math.floorMod(name.trim().lowercase().hashCode(), AvatarPalette.size)]
    Box(
        modifier
            .size(size)
            .clip(CircleShape)
            .background(bg),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            initials(name),
            color = fg,
            fontWeight = FontWeight.Bold,
            fontSize = (size.value * 0.38f).sp,
        )
    }
}

// ─── MoneyField ───────────────────────────────────────────────────────

/** Parses typed text into whole reais: keeps digits only, max 12 digits; empty → null. */
fun parseMoneyDigits(text: String): Long? = digitsOnly(text).take(12).trimStart('0').toLongOrNull()

/** Text field that accepts digits only and always displays the value as "R$ 1.850.000". */
@Composable
fun MoneyField(
    value: Long?,
    onValueChange: (Long?) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    error: String? = null,
    enabled: Boolean = true,
    suffix: String? = null,
) {
    val text = value?.let { formatPrice(it, Currency.BRL) }.orEmpty()
    OutlinedTextField(
        value = TextFieldValue(text, TextRange(text.length)),
        onValueChange = { onValueChange(parseMoneyDigits(it.text)) },
        label = { Text(label) },
        placeholder = { Text("R$ 0") },
        suffix = suffix?.let { { Text(it, color = Regla.Muted) } },
        isError = error != null,
        supportingText = error?.let { { Text(it) } },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        enabled = enabled,
        shape = RoundedCornerShape(Regla.RadiusControl),
        colors = reglaTextFieldColors(),
        textStyle = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
        modifier = modifier.fillMaxWidth(),
    )
}

// ─── ConfirmDialog ────────────────────────────────────────────────────

@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    confirmText: String = "Confirmar",
    dismissText: String = "Cancelar",
    destructive: Boolean = false,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Regla.Surface,
        shape = RoundedCornerShape(24.dp),
        title = { Text(title, style = MaterialTheme.typography.titleLarge, color = Regla.Ink) },
        text = { Text(message, style = MaterialTheme.typography.bodyMedium, color = Regla.Muted) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    confirmText,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (destructive) Regla.Danger else Regla.Coral,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(dismissText, style = MaterialTheme.typography.labelLarge, color = Regla.Muted)
            }
        },
    )
}
