package br.com.imoveisregla.realtor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import br.com.imoveisregla.core.data.AppContainer
import br.com.imoveisregla.core.data.Containers
import br.com.imoveisregla.core.designsystem.ReglaTheme
import br.com.imoveisregla.realtor.ui.RealtorApp

/** Process-wide dependency container (Supabase when configured, demo data otherwise). */
object RealtorGraph {
    val container: AppContainer by lazy { Containers.create(asRealtor = true) }
}

val LocalAppContainer = staticCompositionLocalOf<AppContainer> { error("AppContainer not provided") }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CompositionLocalProvider(LocalAppContainer provides RealtorGraph.container) {
                ReglaTheme { RealtorApp() }
            }
        }
    }
}
