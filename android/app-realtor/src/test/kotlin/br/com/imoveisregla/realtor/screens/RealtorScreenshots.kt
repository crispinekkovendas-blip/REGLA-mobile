package br.com.imoveisregla.realtor.screens

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import br.com.imoveisregla.core.data.fake.FakeBackend
import br.com.imoveisregla.core.designsystem.ReglaTheme
import br.com.imoveisregla.realtor.LocalAppContainer
import br.com.imoveisregla.realtor.feature.auth.LoginScreen
import br.com.imoveisregla.realtor.feature.leads.LeadDetailScreen
import br.com.imoveisregla.realtor.feature.listings.ListingDetailScreen
import br.com.imoveisregla.realtor.feature.proposals.ApplicationDetailScreen
import br.com.imoveisregla.realtor.ui.RealtorApp
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * Renders every realtor-app screen on sample data and writes PNGs to `build/screens/`
 * (r-<screen>.png). CI publishes them to imoveisregla.com.br/admin/app-review.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w393dp-h852dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class RealtorScreenshots {

    @get:Rule val compose = createComposeRule()

    private fun render(db: FakeBackend = FakeBackend(asRealtor = true), content: @Composable () -> Unit) {
        compose.setContent {
            CompositionLocalProvider(LocalAppContainer provides db) { ReglaTheme { content() } }
        }
        settle()
    }

    private fun settle() {
        compose.waitForIdle()
        compose.mainClock.advanceTimeBy(1_500)
        compose.waitForIdle()
    }

    private fun capture(name: String) {
        settle()
        val dir = File("build/screens").apply { mkdirs() }
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        File(dir, "r-$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    /** Bottom-bar labels are the last node with that exact text. */
    private fun tab(label: String) {
        compose.onAllNodesWithText(label).onLast().performClick()
        settle()
    }

    @Test fun tabs() {
        render { RealtorApp() }
        capture("inicio")
        tab("Leads"); capture("leads")
        tab("Propostas"); capture("propostas")
        tab("Agenda"); capture("agenda")
        tab("Imóveis"); capture("imoveis")
    }

    @Test fun login() {
        render(FakeBackend(asRealtor = true, signedIn = false)) { LoginScreen(onLoggedIn = {}) }
        capture("login")
    }

    @Test fun leadDetail() {
        render { LeadDetailScreen(leadId = 101, onBack = {}, onOpenListing = {}) }
        capture("lead-detalhe")
    }

    @Test fun proposalDetail() {
        render { ApplicationDetailScreen(applicationId = 301, onBack = {}, onOpenListing = {}) }
        capture("proposta-detalhe")
    }

    @Test fun listingDetail() {
        render { ListingDetailScreen(listingId = 1, onBack = {}) }
        capture("imovel")
    }
}
