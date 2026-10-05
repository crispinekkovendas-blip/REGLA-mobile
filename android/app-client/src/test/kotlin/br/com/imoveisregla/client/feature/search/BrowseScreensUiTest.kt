package br.com.imoveisregla.client.feature.search

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import br.com.imoveisregla.client.LocalAppContainer
import br.com.imoveisregla.client.feature.favorites.FavoritesScreen
import br.com.imoveisregla.client.feature.listing.ListingDetailScreen
import br.com.imoveisregla.core.data.fake.FakeBackend
import br.com.imoveisregla.core.designsystem.ReglaTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
class BrowseScreensUiTest {

    @get:Rule val compose = createComposeRule()

    private fun waitForText(text: String) {
        compose.waitUntil(timeoutMillis = 5_000) {
            compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test fun searchScreenShowsSeededListings() {
        val backend = FakeBackend(asRealtor = false)
        var opened: Long? = null
        compose.setContent {
            CompositionLocalProvider(LocalAppContainer provides backend) {
                ReglaTheme { SearchScreen(onOpenListing = { opened = it }) }
            }
        }
        compose.onNodeWithText("Encontre seu próximo lar").assertIsDisplayed()
        waitForText("Apartamento com varanda em Pinheiros")
        compose.onNodeWithText("Apartamento com varanda em Pinheiros").assertIsDisplayed()
        compose.onNodeWithText("7 imóveis encontrados").assertIsDisplayed()
        compose.onNodeWithText("Apartamento com varanda em Pinheiros").performClick()
        compose.runOnIdle { assertEquals(1L, opened) }
    }

    @Test fun favoritesSignedOutAsksToLogin() {
        val backend = FakeBackend(asRealtor = false, signedIn = false)
        var loginRequested = false
        compose.setContent {
            CompositionLocalProvider(LocalAppContainer provides backend) {
                ReglaTheme { FavoritesScreen(onOpenListing = {}, onRequireLogin = { loginRequested = true }) }
            }
        }
        waitForText("Entrar")
        compose.onNodeWithText("Entrar").performClick()
        compose.runOnIdle { assertEquals(true, loginRequested) }
    }

    @Test fun listingDetailSignedOutCtaRequiresLogin() {
        val backend = FakeBackend(asRealtor = false, signedIn = false)
        var loginRequested = false
        var applied = false
        compose.setContent {
            CompositionLocalProvider(LocalAppContainer provides backend) {
                ReglaTheme {
                    ListingDetailScreen(
                        listingId = 1, onBack = {}, onBookVisit = {},
                        onApply = { applied = true }, onRequireLogin = { loginRequested = true },
                    )
                }
            }
        }
        waitForText("Fazer proposta")
        compose.onNodeWithText("Apartamento com varanda em Pinheiros").assertExists()
        compose.onNodeWithText("Fazer proposta").performClick()
        compose.runOnIdle {
            assertEquals(true, loginRequested)
            assertEquals(false, applied)
        }
    }
}
