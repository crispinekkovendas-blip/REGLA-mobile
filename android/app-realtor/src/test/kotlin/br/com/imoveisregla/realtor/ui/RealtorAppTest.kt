package br.com.imoveisregla.realtor.ui

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import br.com.imoveisregla.core.data.fake.FakeBackend
import br.com.imoveisregla.core.designsystem.ReglaTheme
import br.com.imoveisregla.realtor.LocalAppContainer
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RealtorAppTest {

    @get:Rule val compose = createComposeRule()

    private fun setApp(fake: FakeBackend) {
        compose.setContent {
            CompositionLocalProvider(LocalAppContainer provides fake) {
                ReglaTheme { RealtorApp() }
            }
        }
    }

    private fun waitForText(text: String) {
        compose.waitUntil(timeoutMillis = 5_000) {
            compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun realtorSeesDashboardWithTabsAndStats() {
        setApp(FakeBackend(asRealtor = true))
        waitForText("Início")
        compose.onAllNodesWithText("Início").onFirst().assertIsDisplayed()
        compose.onAllNodesWithText("Propostas").onFirst().assertExists()
        waitForText("Novos leads")
        compose.onAllNodesWithText("Novos leads").onFirst().assertExists()
        compose.onAllNodesWithText("Imóveis ativos").onFirst().assertExists()
        compose.onAllNodesWithText("Demo").onFirst().assertExists()
    }

    @Test
    fun nonRealtorSeesRestrictedScreen() {
        setApp(FakeBackend(asRealtor = false))
        waitForText("Acesso restrito a corretores REGLA")
        compose.onNodeWithText("Sair").performClick()
        waitForText("Acesso fornecido pela REGLA", substring = true)
    }

    @Test
    fun signedOutRealtorCanLogIn() {
        setApp(FakeBackend(asRealtor = true, signedIn = false))
        waitForText("Acesso fornecido pela REGLA", substring = true)
        compose.onNodeWithText("E-mail").performTextInput("corretor@imoveisregla.com.br")
        compose.onNodeWithText("Senha").performTextInput("segredo123")
        compose.onAllNodesWithText("Entrar")[1].performClick()
        waitForText("Novos leads")
    }

    private fun waitForText(text: String, substring: Boolean) {
        compose.waitUntil(timeoutMillis = 5_000) {
            compose.onAllNodesWithText(text, substring = substring).fetchSemanticsNodes().isNotEmpty()
        }
    }
}
