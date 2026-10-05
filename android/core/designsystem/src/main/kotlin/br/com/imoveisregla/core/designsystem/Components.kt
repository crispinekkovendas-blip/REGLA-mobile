package br.com.imoveisregla.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
    val shape = RoundedCornerShape(Regla.RadiusControl)
    val content: @Composable () -> Unit = {
        if (loading) CircularProgressIndicator(Modifier.height(18.dp), strokeWidth = 2.dp, color = LocalContentColorFor(kind))
        else Text(text, style = MaterialTheme.typography.labelLarge)
    }
    when (kind) {
        ButtonKind.Secondary -> OutlinedButton(
            onClick = onClick, enabled = enabled && !loading, shape = shape,
            modifier = modifier.height(52.dp),
        ) { content() }
        else -> Button(
            onClick = onClick, enabled = enabled && !loading, shape = shape,
            modifier = modifier.height(52.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (kind == ButtonKind.Accent) Regla.Coral else Regla.Navy,
                contentColor = Color.White,
            ),
        ) { content() }
    }
}

@Composable
private fun LocalContentColorFor(kind: ButtonKind): Color = if (kind == ButtonKind.Secondary) Regla.Navy else Color.White

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
        modifier = modifier.fillMaxWidth(),
    )
}

/** Small rounded status label, e.g. "Em análise". */
@Composable
fun StatusPill(text: String, color: Color, background: Color, modifier: Modifier = Modifier) {
    Text(
        text = text,
        color = color,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(background)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(text, style = MaterialTheme.typography.titleMedium, color = Regla.Ink, modifier = modifier)
}

@Composable
fun LoadingState(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Regla.Coral) }
}

@Composable
fun ErrorState(message: String, onRetry: (() -> Unit)? = null, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Algo deu errado", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(6.dp))
        Text(message, color = Regla.Muted, textAlign = TextAlign.Center)
        if (onRetry != null) {
            Spacer(Modifier.height(16.dp))
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
        Text(title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(message, color = Regla.Muted, textAlign = TextAlign.Center)
        if (action != null) {
            Spacer(Modifier.height(20.dp))
            action()
        }
    }
}

/** Listing photo with a branded placeholder when there is no URL. */
@Composable
fun ListingImage(url: String?, contentDescription: String?, modifier: Modifier = Modifier) {
    Box(modifier.background(Regla.NavySoft), contentAlignment = Alignment.Center) {
        Icon(Icons.Outlined.Home, contentDescription = null, tint = Regla.Muted)
        if (url != null) {
            AsyncImage(
                model = url, contentDescription = contentDescription, contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

/** Banner shown when the app runs on demo data (no Supabase configured). */
@Composable
fun DemoBanner(modifier: Modifier = Modifier) {
    Text(
        "Modo demonstração · dados de exemplo",
        color = Regla.Ink,
        style = MaterialTheme.typography.labelMedium,
        textAlign = TextAlign.Center,
        modifier = modifier.fillMaxWidth().background(Regla.WarnSoft).padding(vertical = 6.dp),
    )
}
