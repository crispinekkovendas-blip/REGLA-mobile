package br.com.imoveisregla.client.ui

import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import br.com.imoveisregla.client.LocalAppContainer
import br.com.imoveisregla.client.nav.FavoritesRoute
import br.com.imoveisregla.client.nav.LoginRoute
import br.com.imoveisregla.core.data.fake.FakeBackend
import br.com.imoveisregla.core.designsystem.ReglaTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runner.RunWith
import org.junit.runners.model.Statement
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Compose tests need the ComponentActivity host declared by ui-test-manifest, which the app only
 * adds as debugImplementation. `./gradlew test` also runs the release unit tests, so skip there.
 */
private class RequiresComposeTestActivity : TestRule {
    override fun apply(base: Statement, description: Description): Statement = object : Statement() {
        override fun evaluate() {
            val app = RuntimeEnvironment.getApplication()
            val intent = Intent().setClassName(app.packageName, ComponentActivity::class.java.name)
            Assume.assumeTrue(
                "ComponentActivity not in the test manifest (release variant)",
                app.packageManager.resolveActivity(intent, 0) != null,
            )
            base.evaluate()
        }
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ClientAppTest {

    private val compose = createComposeRule()

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(RequiresComposeTestActivity()).around(compose)

    private lateinit var nav: NavHostController

    private fun launch(signedIn: Boolean = true) {
        compose.setContent {
            CompositionLocalProvider(LocalAppContainer provides FakeBackend(asRealtor = false, signedIn = signedIn)) {
                ReglaTheme {
                    nav = rememberNavController()
                    ClientApp(nav)
                }
            }
        }
    }

    @Test fun rendersBottomBarLabelsAndDemoBanner() {
        launch()
        assertEquals(listOf("Buscar", "Favoritos", "Visitas", "Propostas", "Perfil"), ClientTab.entries.map { it.label })
        ClientTab.entries.forEach { tab ->
            compose.onNodeWithTag(tab.testTag).assertIsDisplayed().assert(hasText(tab.label))
        }
        compose.onNodeWithTag(ClientTab.Search.testTag).assertIsSelected()
        compose.onNodeWithText("Modo demonstração · dados de exemplo").assertIsDisplayed()
    }

    @Test fun tappingFavoritosNavigatesToFavorites() {
        launch()
        compose.onNodeWithTag(ClientTab.Favorites.testTag).performClick()
        compose.waitForIdle()

        compose.onNodeWithTag(ClientTab.Favorites.testTag).assertIsSelected()
        compose.onNodeWithTag(ClientTab.Search.testTag).assertIsNotSelected()
        assertTrue(nav.currentDestination?.hasRoute(FavoritesRoute::class) == true)
    }

    @Test fun stackScreensHideBottomBar() {
        launch(signedIn = false)
        compose.onNodeWithTag(BOTTOM_BAR_TAG).assertIsDisplayed()

        compose.runOnUiThread { nav.navigate(LoginRoute) }
        compose.waitForIdle()

        compose.onNodeWithTag(BOTTOM_BAR_TAG).assertDoesNotExist()
        compose.onNodeWithText("Seu próximo lar, sem burocracia").assertIsDisplayed()

        compose.runOnUiThread { nav.popBackStack() }
        compose.waitForIdle()
        compose.onNodeWithTag(BOTTOM_BAR_TAG).assertIsDisplayed()
    }
}
