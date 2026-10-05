package br.com.imoveisregla.client

import android.graphics.Color
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import br.com.imoveisregla.client.ui.ClientApp
import br.com.imoveisregla.core.data.AppContainer
import br.com.imoveisregla.core.data.Containers
import br.com.imoveisregla.core.designsystem.ReglaTheme

/** Process-wide dependency container (Supabase when configured, demo data otherwise). */
object ClientGraph {
    val container: AppContainer by lazy { Containers.create(asRealtor = false) }
}

val LocalAppContainer = staticCompositionLocalOf<AppContainer> { error("AppContainer not provided") }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // The app uses a light palette only: keep dark system-bar icons even when the device is in dark mode.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        setContent {
            CompositionLocalProvider(LocalAppContainer provides ClientGraph.container) {
                ReglaTheme { ClientApp() }
            }
        }
    }
}
