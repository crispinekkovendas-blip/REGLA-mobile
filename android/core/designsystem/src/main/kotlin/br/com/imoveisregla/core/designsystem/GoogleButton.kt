package br.com.imoveisregla.core.designsystem

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

const val GOOGLE_BUTTON_TAG = "google-sign-in"

/** "Continuar com o Google" — white outlined button with a Google "G" mark. */
@Composable
fun GoogleSignInButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    loading: Boolean = false,
    enabled: Boolean = true,
    text: String = "Continuar com o Google",
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled && !loading,
        shape = RoundedCornerShape(Regla.RadiusControl),
        border = BorderStroke(1.dp, Regla.Line),
        colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White, contentColor = Regla.Ink),
        modifier = modifier.fillMaxWidth().height(52.dp).testTag(GOOGLE_BUTTON_TAG),
    ) {
        if (loading) {
            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = Regla.Navy)
        } else {
            Box(
                Modifier.size(22.dp).clip(CircleShape).background(Color(0xFFF1F3F4)),
                contentAlignment = Alignment.Center,
            ) {
                Text("G", color = Color(0xFF4285F4), fontWeight = FontWeight.Black, fontSize = 15.sp)
            }
            Spacer(Modifier.width(10.dp))
            Text(text, style = MaterialTheme.typography.labelLarge)
        }
    }
}
