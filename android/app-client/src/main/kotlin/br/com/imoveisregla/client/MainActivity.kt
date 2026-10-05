package br.com.imoveisregla.client

import android.os.Bundle
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
        enableEdgeToEdge()
        setContent {
            CompositionLocalProvider(LocalAppContainer provides ClientGraph.container) {
                ReglaTheme { ClientApp() }
            }
        }
    }
}
