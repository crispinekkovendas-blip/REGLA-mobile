package br.com.imoveisregla.client.feature.profile

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import br.com.imoveisregla.client.LocalAppContainer
import br.com.imoveisregla.core.data.fake.FakeBackend
import br.com.imoveisregla.core.designsystem.ReglaTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ProfileScreenTest {

    @get:Rule val rule = createComposeRule()

    private fun waitForText(text: String) {
        rule.waitUntil(5_000) { rule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() }
    }

    @Test fun signedIn_showsSeededClient() {
        var edited = false
        rule.setContent {
            CompositionLocalProvider(LocalAppContainer provides FakeBackend(asRealtor = false)) {
                ReglaTheme { ProfileScreen(onEditProfile = { edited = true }, onDocuments = {}, onLogin = {}) }
            }
        }
        waitForText("Mariana Souza")
        rule.onNodeWithText("Mariana Souza").assertIsDisplayed()
        rule.onNodeWithText("Cadastro 100% completo").assertIsDisplayed()
        rule.onNodeWithText("Meu cadastro").performClick()
        assertTrue(edited)
    }

    @Test fun signedOut_showsLoginCta() {
        var login = false
        rule.setContent {
            CompositionLocalProvider(LocalAppContainer provides FakeBackend(asRealtor = false, signedIn = false)) {
                ReglaTheme { ProfileScreen(onEditProfile = {}, onDocuments = {}, onLogin = { login = true }) }
            }
        }
        rule.onNodeWithText("Entrar ou criar conta").performClick()
        assertTrue(login)
    }

    @Test fun documentsScreen_listsRequiredKinds() {
        rule.setContent {
            CompositionLocalProvider(LocalAppContainer provides FakeBackend(asRealtor = false)) {
                ReglaTheme { DocumentsScreen(onBack = {}) }
            }
        }
        waitForText("RG ou CNH")
        rule.onNodeWithText("0 de 3 documentos obrigatórios enviados").assertIsDisplayed()
    }
}
