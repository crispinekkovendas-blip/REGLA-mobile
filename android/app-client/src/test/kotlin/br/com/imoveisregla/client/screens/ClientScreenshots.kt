package br.com.imoveisregla.client.screens

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import br.com.imoveisregla.client.LocalAppContainer
import br.com.imoveisregla.client.feature.apply.ApplyScreen
import br.com.imoveisregla.client.feature.auth.LoginScreen
import br.com.imoveisregla.client.feature.auth.SignupScreen
import br.com.imoveisregla.client.feature.listing.ListingDetailScreen
import br.com.imoveisregla.client.feature.profile.DocumentsScreen
import br.com.imoveisregla.client.feature.profile.EditProfileScreen
import br.com.imoveisregla.client.feature.proposals.ProposalDetailScreen
import br.com.imoveisregla.client.feature.visit.BookVisitScreen
import br.com.imoveisregla.client.ui.ClientApp
import br.com.imoveisregla.client.ui.ClientTab
import br.com.imoveisregla.core.data.fake.FakeBackend
import br.com.imoveisregla.core.designsystem.ReglaTheme
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * Renders every client-app screen on sample data and writes PNGs to `build/screens/`
 * (c-<screen>.png). CI publishes them to imoveisregla.com.br/admin/app-review.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w393dp-h852dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ClientScreenshots {

    @get:Rule val compose = createComposeRule()

    private fun render(db: FakeBackend = FakeBackend(asRealtor = false), content: @Composable () -> Unit) {
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
        File(dir, "c-$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private fun tab(tab: ClientTab) {
        compose.onNodeWithTag(tab.testTag).performClick()
        settle()
    }

    @Test fun tabs() {
        val db = FakeBackend(asRealtor = false)
        runBlocking { db.favorites.set(1, true); db.favorites.set(3, true) }
        render(db) { ClientApp() }
        capture("busca")
        tab(ClientTab.Favorites); capture("favoritos")
        tab(ClientTab.Visits); capture("visitas")
        tab(ClientTab.Proposals); capture("propostas")
        tab(ClientTab.Profile); capture("perfil")
    }

    @Test fun listingDetail() {
        render { ListingDetailScreen(listingId = 1, onBack = {}, onBookVisit = {}, onApply = {}, onRequireLogin = {}) }
        capture("imovel")
    }

    @Test fun bookVisit() {
        render { BookVisitScreen(listingId = 1, onBack = {}, onDone = {}) }
        capture("agendar")
    }

    @Test fun proposalWizard() {
        render { ApplyScreen(listingId = 2, onBack = {}, onDone = {}) }
        capture("proposta-1-dados")
        compose.onNodeWithText("Continuar").performClick()
        capture("proposta-2-oferta")
        compose.onNodeWithText("Continuar").performClick()
        capture("proposta-3-revisao")
    }

    @Test fun proposalDetail() {
        render { ProposalDetailScreen(applicationId = 302, onBack = {}, onOpenListing = {}) }
        capture("proposta-detalhe")
    }

    @Test fun editProfile() {
        render { EditProfileScreen(onBack = {}) }
        capture("cadastro")
    }

    @Test fun documents() {
        render { DocumentsScreen(onBack = {}) }
        capture("documentos")
    }

    @Test fun login() {
        render(FakeBackend(asRealtor = false, signedIn = false)) { LoginScreen(onLoggedIn = {}, onSignup = {}, onBack = {}) }
        capture("login")
    }

    @Test fun signup() {
        render(FakeBackend(asRealtor = false, signedIn = false)) { SignupScreen(onSignedUp = {}, onBack = {}) }
        capture("criar-conta")
    }
}
