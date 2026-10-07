package br.com.imoveisregla.realtor.feature.proposals

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertAny
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import br.com.imoveisregla.core.data.fake.FakeBackend
import br.com.imoveisregla.core.designsystem.ReglaTheme
import br.com.imoveisregla.realtor.LocalAppContainer
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ProposalsScreenTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun showsSeededProposalInReview_andFilters() {
        val fake = FakeBackend(asRealtor = true)
        var opened: Long? = null
        compose.setContent {
            CompositionLocalProvider(LocalAppContainer provides fake) {
                ReglaTheme { ProposalsScreen(onOpenApplication = { opened = it }) }
            }
        }
        compose.waitForIdle()

        compose.onNodeWithText("Apartamento com varanda em Pinheiros").assertIsDisplayed()
        compose.onNodeWithTag("status-301", useUnmergedTree = true).onChildren().assertAny(hasText("Em análise"))
        compose.onNodeWithText("Todas (2)").assertExists()
        compose.onNodeWithText("Sua vez (1)").assertExists()
        compose.onNodeWithText("-4,2%").assertExists()

        compose.onNodeWithTag("proposal-301").performClick()
        assertEquals(301L, opened)

        compose.onNodeWithTag("filter-APPROVED").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Nada por aqui").assertIsDisplayed()
        compose.onNodeWithTag("proposal-301").assertDoesNotExist()

        compose.onNodeWithText("Ver todas").performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("proposal-301").assertIsDisplayed()
    }
}
