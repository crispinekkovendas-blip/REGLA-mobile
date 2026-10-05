package br.com.imoveisregla.core.designsystem

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** REGLA brand tokens (match the imoveisregla.com.br web app). */
object Regla {
    val Navy = Color(0xFF0B1F3A)
    val NavySoft = Color(0xFFF1F4F9)
    val Coral = Color(0xFFFF6B35)
    val CoralSoft = Color(0xFFFFEDE5)
    val Ink = Color(0xFF1A2233)
    val Muted = Color(0xFF5C6478)
    val Line = Color(0xFFE5E8EF)
    val Surface = Color(0xFFFFFFFF)
    val Ok = Color(0xFF1FA971)
    val OkSoft = Color(0xFFE3F7EE)
    val Warn = Color(0xFFE59A1A)
    val WarnSoft = Color(0xFFFFF4E0)
    val Danger = Color(0xFFDC2626)
    val DangerSoft = Color(0xFFFEF2F2)
    val Info = Color(0xFF2F8FE0)
    val InfoSoft = Color(0xFFE6F2FD)

    val Gutter = 16.dp
    val RadiusCard = 16.dp
    val RadiusControl = 12.dp
}

private val ReglaColors = lightColorScheme(
    primary = Regla.Navy,
    onPrimary = Color.White,
    primaryContainer = Regla.NavySoft,
    onPrimaryContainer = Regla.Navy,
    secondary = Regla.Coral,
    onSecondary = Color.White,
    secondaryContainer = Regla.CoralSoft,
    onSecondaryContainer = Regla.Ink,
    background = Regla.NavySoft,
    onBackground = Regla.Ink,
    surface = Regla.Surface,
    onSurface = Regla.Ink,
    surfaceVariant = Regla.NavySoft,
    onSurfaceVariant = Regla.Muted,
    outline = Regla.Line,
    outlineVariant = Regla.Line,
    error = Regla.Danger,
)

private val ReglaType = Typography(
    headlineLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 30.sp, lineHeight = 36.sp, letterSpacing = (-0.5).sp),
    headlineMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 30.sp, letterSpacing = (-0.3).sp),
    titleLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 26.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 15.sp, letterSpacing = 0.2.sp),
    labelMedium = TextStyle(fontWeight = FontWeight.Medium, fontSize = 12.sp, letterSpacing = 0.3.sp),
)

private val ReglaShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(Regla.RadiusControl),
    large = RoundedCornerShape(Regla.RadiusCard),
)

@Composable
fun ReglaTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = ReglaColors, typography = ReglaType, shapes = ReglaShapes, content = content)
}
