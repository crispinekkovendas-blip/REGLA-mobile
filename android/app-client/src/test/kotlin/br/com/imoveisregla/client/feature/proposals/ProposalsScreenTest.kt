package br.com.imoveisregla.client.feature.proposals

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import br.com.imoveisregla.client.LocalAppContainer
import br.com.imoveisregla.core.data.fake.FakeBackend
import br.com.imoveisregla.core.designsystem.ReglaTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ProposalsScreenTest {

    @get:Rule val compose = createComposeRule()

    @Test fun showsSeededProposalInReview() {
        val db = FakeBackend(asRealtor = false)
        var opened: Long? = null
        var openedProposal: Long? = null
        compose.setContent {
            CompositionLocalProvider(LocalAppContainer provides db) {
                ReglaTheme {
                    ProposalsScreen(onOpenListing = { opened = it }, onRequireLogin = {}, onOpenProposal = { openedProposal = it })
                }
            }
        }
        compose.waitUntil(5_000) {
            compose.onAllNodesWithText("Apartamento com varanda em Pinheiros").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onAllNodesWithText("Em análise").onFirst().assertIsDisplayed()
        compose.onNodeWithText("R$ 4.600").assertIsDisplayed()
        compose.onAllNodesWithText("Cancelar proposta").onFirst().assertIsDisplayed()

        // tapping the card opens the proposta detail
        compose.onNodeWithTag("proposal-301").performClick()
        assertEquals(301L, openedProposal)

        compose.onNodeWithText("Apartamento com varanda em Pinheiros").performClick()
        assertEquals(1L, opened)
    }

    @Test fun signedOutShowsLogin() {
        val db = FakeBackend(asRealtor = false, signedIn = false)
        var loginRequested = false
        compose.setContent {
            CompositionLocalProvider(LocalAppContainer provides db) {
                ReglaTheme { ProposalsScreen(onOpenListing = {}, onRequireLogin = { loginRequested = true }, onOpenProposal = {}) }
            }
        }
        compose.onNodeWithText("Entrar").performClick()
        assertEquals(true, loginRequested)
    }
}
