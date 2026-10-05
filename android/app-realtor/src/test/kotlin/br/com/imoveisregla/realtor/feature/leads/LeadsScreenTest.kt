package br.com.imoveisregla.realtor.feature.leads

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
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
class LeadsScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test fun showsSeededLeadAndFiltersBySearch() {
        compose.setContent {
            CompositionLocalProvider(LocalAppContainer provides FakeBackend(asRealtor = true)) {
                ReglaTheme { LeadsScreen(onOpenLead = {}) }
            }
        }
        compose.onNodeWithText("Leads").assertIsDisplayed()
        compose.onNodeWithText("Carlos Lima").assertIsDisplayed()
        compose.onNodeWithText("Todos · 6").assertIsDisplayed()

        compose.onNode(hasSetTextAction()).performTextInput("Fernanda")
        compose.waitForIdle()
        compose.onNodeWithText("Fernanda Alves").assertIsDisplayed()
        compose.onNodeWithText("Carlos Lima").assertDoesNotExist()
    }
}
