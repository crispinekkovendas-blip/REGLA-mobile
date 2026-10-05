package br.com.imoveisregla.client.feature.visits

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import br.com.imoveisregla.client.LocalAppContainer
import br.com.imoveisregla.core.data.fake.FakeBackend
import br.com.imoveisregla.core.designsystem.ReglaTheme
import br.com.imoveisregla.core.model.ShowingStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class VisitsScreenTest {
    @get:Rule val rule = createComposeRule()

    @Test fun showsSeededVisitAndCancelsIt() {
        val fb = FakeBackend(asRealtor = false)
        var opened: Long? = null
        rule.setContent {
            CompositionLocalProvider(LocalAppContainer provides fb) {
                ReglaTheme { VisitsScreen(onOpenListing = { opened = it }, onRequireLogin = {}) }
            }
        }
        rule.onNodeWithText("Minhas visitas").assertIsDisplayed()
        rule.onNodeWithText("Próximas").assertIsDisplayed()
        rule.onNodeWithText("Agendada").assertIsDisplayed()
        rule.onNodeWithText("Apartamento com varanda em Pinheiros").assertIsDisplayed().performClick()
        assertEquals(1L, opened)

        rule.onNodeWithText("Cancelar visita").performClick()
        rule.onNodeWithText("Cancelar visita?").assertIsDisplayed()
        rule.onNodeWithText("Sim, cancelar").performClick()
        rule.waitForIdle()

        rule.onNodeWithText("Anteriores").assertIsDisplayed()
        rule.onNodeWithText("Cancelada").assertIsDisplayed()
        assertEquals(ShowingStatus.CANCELLED, fb.showingRows.first { it.id == 203L }.status)
    }

    @Test fun signedOutAsksToLogIn() {
        var asked = false
        rule.setContent {
            CompositionLocalProvider(LocalAppContainer provides FakeBackend(asRealtor = false, signedIn = false)) {
                ReglaTheme { VisitsScreen(onOpenListing = {}, onRequireLogin = { asked = true }) }
            }
        }
        rule.onNodeWithText("Entrar").performClick()
        assertTrue(asked)
    }

    @Test fun emptyStateWhenNoVisits() {
        rule.setContent {
            CompositionLocalProvider(LocalAppContainer provides FakeBackend(asRealtor = false, seed = false)) {
                ReglaTheme { VisitsScreen(onOpenListing = {}, onRequireLogin = {}) }
            }
        }
        rule.onNodeWithText("Nenhuma visita agendada").assertIsDisplayed()
    }
}
