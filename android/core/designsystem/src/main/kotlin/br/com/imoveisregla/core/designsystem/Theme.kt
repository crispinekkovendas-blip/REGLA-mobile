package br.com.imoveisregla.core.designsystem

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** REGLA brand tokens (match the imoveisregla.com.br web app). */
object Regla {
    // Brand
    val Navy = Color(0xFF0B1F3A)
    val NavyDeep = Color(0xFF06142A)
    val NavyMid = Color(0xFF1C3560)
    val NavySoft = Color(0xFFF1F4F9)
    val Coral = Color(0xFFFF6B35)
    val CoralDeep = Color(0xFFE8551F)
    val CoralInk = Color(0xFFB8410F)
    val CoralSoft = Color(0xFFFFEDE5)

    // Neutrals
    val Ink = Color(0xFF1A2233)
    val Muted = Color(0xFF5C6478)
    val Subtle = Color(0xFF8A91A3)
    val Line = Color(0xFFE5E8EF)
    val Surface = Color(0xFFFFFFFF)
    val Canvas = Color(0xFFF6F7FB)

    // Semantic (base / readable text / soft background)
    val Ok = Color(0xFF1FA971)
    val OkInk = Color(0xFF117A4E)
    val OkSoft = Color(0xFFE3F7EE)
    val Warn = Color(0xFFE59A1A)
    val WarnInk = Color(0xFF94590A)
    val WarnSoft = Color(0xFFFFF4E0)
    val Danger = Color(0xFFDC2626)
    val DangerInk = Color(0xFFB01C1C)
    val DangerSoft = Color(0xFFFEF2F2)
    val Info = Color(0xFF2F8FE0)
    val InfoInk = Color(0xFF1B65A8)
    val InfoSoft = Color(0xFFE6F2FD)

    // Layout
    val Gutter = 16.dp
    val RadiusCard = 16.dp
    val RadiusControl = 12.dp
    val RadiusPill = 50
    val Space1 = 4.dp
    val Space2 = 8.dp
    val Space3 = 12.dp
    val Space4 = 16.dp
    val Space5 = 24.dp
    val Space6 = 32.dp
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
    tertiary = Regla.NavyMid,
    onTertiary = Color.White,
    background = Regla.Canvas,
    onBackground = Regla.Ink,
    surface = Regla.Surface,
    onSurface = Regla.Ink,
    surfaceVariant = Regla.NavySoft,
    onSurfaceVariant = Regla.Muted,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color.White,
    surfaceContainer = Color.White,
    surfaceContainerHigh = Regla.NavySoft,
    surfaceContainerHighest = Regla.NavySoft,
    outline = Regla.Line,
    outlineVariant = Regla.Line,
    error = Regla.Danger,
    onError = Color.White,
    errorContainer = Regla.DangerSoft,
    onErrorContainer = Regla.DangerInk,
)

private val Sans = FontFamily.SansSerif

private val ReglaType = Typography(
    displaySmall = TextStyle(fontFamily = Sans, fontWeight = FontWeight.ExtraBold, fontSize = 34.sp, lineHeight = 40.sp, letterSpacing = (-1).sp),
    headlineLarge = TextStyle(fontFamily = Sans, fontWeight = FontWeight.ExtraBold, fontSize = 30.sp, lineHeight = 36.sp, letterSpacing = (-0.8).sp),
    headlineMedium = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 30.sp, letterSpacing = (-0.5).sp),
    headlineSmall = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Bold, fontSize = 21.sp, lineHeight = 27.sp, letterSpacing = (-0.3).sp),
    titleLarge = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Bold, fontSize = 19.sp, lineHeight = 25.sp, letterSpacing = (-0.2).sp),
    titleMedium = TextStyle(fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp, letterSpacing = (-0.1).sp),
    titleSmall = TextStyle(fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp),
    bodyLarge = TextStyle(fontFamily = Sans, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = Sans, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontFamily = Sans, fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Bold, fontSize = 15.sp, letterSpacing = 0.2.sp),
    labelMedium = TextStyle(fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, letterSpacing = 0.3.sp),
    labelSmall = TextStyle(fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, letterSpacing = 0.8.sp),
)

private val ReglaShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(Regla.RadiusControl),
    large = RoundedCornerShape(Regla.RadiusCard),
    extraLarge = RoundedCornerShape(24.dp),
)

@Composable
fun ReglaTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = ReglaColors, typography = ReglaType, shapes = ReglaShapes, content = content)
}
