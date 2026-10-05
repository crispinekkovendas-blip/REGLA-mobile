package br.com.imoveisregla.client.feature.apply

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import br.com.imoveisregla.core.designsystem.Regla
import br.com.imoveisregla.core.designsystem.StatusPill
import br.com.imoveisregla.core.model.Affordability
import br.com.imoveisregla.core.model.label

/**
 * Text field whose model is raw digits but which displays a formatted value
 * (CPF, phone, money). The cursor is kept at the end so masking never jumps it.
 */
@Composable
fun MaskedDigitsField(
    digits: String,
    onDigitsChange: (String) -> Unit,
    label: String,
    format: (String) -> String,
    maxDigits: Int,
    modifier: Modifier = Modifier,
    error: String? = null,
    supporting: String? = null,
    stripLeadingZeros: Boolean = false,
) {
    val display = if (digits.isEmpty()) "" else format(digits)
    OutlinedTextField(
        value = TextFieldValue(display, selection = TextRange(display.length)),
        onValueChange = { tfv ->
            val d = tfv.text.filter { it.isDigit() }
            onDigitsChange((if (stripLeadingZeros) d.trimStart('0') else d).take(maxDigits))
        },
        label = { Text(label) },
        isError = error != null,
        supportingText = (error ?: supporting)?.let { msg -> { Text(msg) } },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        shape = RoundedCornerShape(Regla.RadiusControl),
        modifier = modifier.fillMaxWidth(),
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun <T> ChoiceChips(
    options: List<T>,
    selected: T?,
    label: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        options.forEach { option ->
            val isSelected = option == selected
            FilterChip(
                selected = isSelected,
                onClick = { onSelect(option) },
                label = { Text(label(option)) },
                leadingIcon = if (isSelected) {
                    { Icon(Icons.Outlined.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                } else null,
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Regla.Navy,
                    selectedLabelColor = Color.White,
                    selectedLeadingIconColor = Color.White,
                ),
            )
        }
    }
}

@Composable
fun FieldError(text: String?) {
    if (text != null) {
        Text(text, color = Regla.Danger, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 2.dp))
    }
}

@Composable
fun FieldLabel(text: String) {
    Text(text, style = MaterialTheme.typography.labelLarge, color = Regla.Ink, modifier = Modifier.padding(bottom = 6.dp))
}

fun affordabilityColors(a: Affordability): Pair<Color, Color> = when (a) {
    Affordability.OK -> Regla.Ok to Regla.OkSoft
    Affordability.TIGHT -> Regla.Warn to Regla.WarnSoft
    Affordability.OVER -> Regla.Danger to Regla.DangerSoft
    Affordability.UNKNOWN -> Regla.Muted to Regla.NavySoft
}

/** Colored pill showing whether the rent fits the household income. */
@Composable
fun WizardAffordabilityBadge(affordability: Affordability, modifier: Modifier = Modifier) {
    val (fg, bg) = affordabilityColors(affordability)
    StatusPill(text = affordability.label, color = fg, background = bg, modifier = modifier)
}

/** "Cadastro · Proposta · Documentos · Revisão" + progress bar. */
@Composable
fun WizardStepIndicator(current: WizardStep, modifier: Modifier = Modifier) {
    val steps = WizardStep.entries
    Column(modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            steps.forEach { step ->
                val done = step.ordinal < current.ordinal
                val active = step == current
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                    Box(
                        Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    done -> Regla.Ok
                                    active -> Regla.Navy
                                    else -> Regla.Surface
                                },
                            )
                            .border(BorderStroke(1.dp, if (done || active) Color.Transparent else Regla.Line), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (done) Icon(Icons.Outlined.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        else Text(
                            "${step.ordinal + 1}",
                            color = if (active) Color.White else Regla.Muted,
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        step.title,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (active) Regla.Ink else Regla.Muted,
                        fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        LinearProgressIndicator(
            progress = { (current.ordinal + 1f) / steps.size },
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(50)),
            color = Regla.Coral,
            trackColor = Regla.Line,
            drawStopIndicator = {},
        )
    }
}

/** Soft colored notice box (warnings, info). */
@Composable
fun NoticeBox(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Regla.Warn,
    background: Color = Regla.WarnSoft,
    title: String? = null,
) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Regla.RadiusControl))
            .background(background)
            .padding(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(Icons.Outlined.WarningAmber, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Column {
            if (title != null) Text(title, style = MaterialTheme.typography.labelLarge, color = Regla.Ink)
            Text(text, style = MaterialTheme.typography.bodyMedium, color = Regla.Ink)
        }
    }
}

@Composable
fun WizardCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Regla.RadiusCard),
        colors = CardDefaults.cardColors(containerColor = Regla.Surface),
        border = BorderStroke(1.dp, Regla.Line),
    ) {
        Column(Modifier.padding(16.dp)) { content() }
    }
}
