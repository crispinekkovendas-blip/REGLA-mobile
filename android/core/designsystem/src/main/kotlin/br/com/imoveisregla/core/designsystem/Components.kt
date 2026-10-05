package br.com.imoveisregla.core.designsystem

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage

enum class ButtonKind { Primary, Accent, Secondary }

@Composable
fun ReglaButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    kind: ButtonKind = ButtonKind.Primary,
    enabled: Boolean = true,
    loading: Boolean = false,
) {
    val shape = RoundedCornerShape(Regla.RadiusControl + 2.dp)
    val content: @Composable () -> Unit = {
        if (loading) {
            CircularProgressIndicator(
                Modifier.size(20.dp),
                strokeWidth = 2.5.dp,
                color = if (kind == ButtonKind.Secondary) Regla.Navy else Color.White,
            )
        } else {
            Text(text, style = MaterialTheme.typography.labelLarge, maxLines = 1)
        }
    }
    when (kind) {
        ButtonKind.Secondary -> OutlinedButton(
            onClick = onClick,
            enabled = enabled && !loading,
            shape = shape,
            border = BorderStroke(1.5.dp, if (enabled) Regla.Navy else Regla.Line),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = Color.White,
                contentColor = Regla.Navy,
                disabledContentColor = Regla.Subtle,
            ),
            modifier = modifier.height(52.dp),
        ) { content() }
        else -> Button(
            onClick = onClick,
            enabled = enabled && !loading,
            shape = shape,
            modifier = modifier.height(52.dp),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 0.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (kind == ButtonKind.Accent) Regla.Coral else Regla.Navy,
                contentColor = Color.White,
                disabledContainerColor = if (loading) {
                    if (kind == ButtonKind.Accent) Regla.Coral.copy(alpha = 0.75f) else Regla.Navy.copy(alpha = 0.75f)
                } else {
                    Regla.Line
                },
                disabledContentColor = if (loading) Color.White else Regla.Subtle,
            ),
        ) { content() }
    }
}

@Composable
fun ReglaTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    error: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = true,
    minLines: Int = 1,
    enabled: Boolean = true,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        isError = error != null,
        supportingText = error?.let { { Text(it) } },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        singleLine = singleLine,
        minLines = minLines,
        enabled = enabled,
        shape = RoundedCornerShape(Regla.RadiusControl),
        colors = reglaTextFieldColors(),
        modifier = modifier.fillMaxWidth(),
    )
}

@Composable
internal fun reglaTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Regla.Navy,
    unfocusedBorderColor = Regla.Line,
    disabledBorderColor = Regla.Line,
    errorBorderColor = Regla.Danger,
    focusedLabelColor = Regla.Navy,
    unfocusedLabelColor = Regla.Muted,
    errorLabelColor = Regla.Danger,
    errorSupportingTextColor = Regla.Danger,
    cursorColor = Regla.Coral,
    focusedContainerColor = Color.White,
    unfocusedContainerColor = Color.White,
    disabledContainerColor = Regla.NavySoft,
    errorContainerColor = Color.White,
    focusedTextColor = Regla.Ink,
    unfocusedTextColor = Regla.Ink,
)

/** Small rounded status label, e.g. "Em análise". A leading dot in [color] marks the state. */
@Composable
fun StatusPill(text: String, color: Color, background: Color, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(Regla.RadiusPill))
            .background(background)
            .padding(start = 8.dp, end = 10.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(color),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = text,
            color = color,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
    }
}

/** Section heading with a short coral accent bar. */
@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .width(4.dp)
                .height(18.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Regla.Coral),
        )
        Spacer(Modifier.width(10.dp))
        Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Regla.Ink)
    }
}

@Composable
fun LoadingState(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = Regla.Coral, trackColor = Regla.CoralSoft, strokeWidth = 3.dp)
    }
}

@Composable
private fun StateIcon(icon: ImageVector, tint: Color, background: Color) {
    Box(
        Modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(background),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(32.dp))
    }
}

@Composable
fun ErrorState(message: String, onRetry: (() -> Unit)? = null, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        StateIcon(Icons.Outlined.ErrorOutline, Regla.Danger, Regla.DangerSoft)
        Spacer(Modifier.height(16.dp))
        Text("Algo deu errado", style = MaterialTheme.typography.titleLarge, color = Regla.Ink)
        Spacer(Modifier.height(6.dp))
        Text(message, color = Regla.Muted, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyMedium)
        if (onRetry != null) {
            Spacer(Modifier.height(20.dp))
            ReglaButton("Tentar novamente", onRetry, kind = ButtonKind.Secondary)
        }
    }
}

@Composable
fun EmptyState(title: String, message: String, modifier: Modifier = Modifier, action: (@Composable () -> Unit)? = null) {
    Column(
        modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        StateIcon(Icons.Outlined.Inbox, Regla.Navy, Regla.NavySoft)
        Spacer(Modifier.height(16.dp))
        Text(title, style = MaterialTheme.typography.titleLarge, color = Regla.Ink, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(message, color = Regla.Muted, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyMedium)
        if (action != null) {
            Spacer(Modifier.height(20.dp))
            action()
        }
    }
}

/** Listing photo with a branded placeholder when there is no URL. */
@Composable
fun ListingImage(url: String?, contentDescription: String?, modifier: Modifier = Modifier) {
    Box(
        modifier.background(Brush.linearGradient(listOf(Regla.NavySoft, Regla.Line))),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Outlined.Home, contentDescription = null, tint = Regla.Subtle, modifier = Modifier.size(36.dp))
        if (url != null) {
            AsyncImage(
                model = url,
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

/** Banner shown when the app runs on demo data (no Supabase configured). */
@Composable
fun DemoBanner(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Regla.Navy)
            .padding(vertical = 6.dp, horizontal = Regla.Gutter),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Outlined.Science, contentDescription = null, tint = Regla.Coral, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(6.dp))
        Text(
            "Modo demonstração · dados de exemplo",
            color = Color.White,
            style = MaterialTheme.typography.labelMedium,
            textAlign = TextAlign.Center,
        )
    }
}
