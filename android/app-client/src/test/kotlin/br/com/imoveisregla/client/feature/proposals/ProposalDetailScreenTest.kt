package br.com.imoveisregla.client.feature.proposals

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import br.com.imoveisregla.client.LocalAppContainer
import br.com.imoveisregla.core.data.fake.FakeBackend
import br.com.imoveisregla.core.designsystem.ReglaTheme
import br.com.imoveisregla.core.model.ApplicationStatus
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ProposalDetailScreenTest {

    @get:Rule val compose = createComposeRule()

    private fun show(db: FakeBackend, id: Long) {
        compose.setContent {
            CompositionLocalProvider(LocalAppContainer provides db) {
                ReglaTheme { ProposalDetailScreen(applicationId = id, onBack = {}, onOpenListing = {}) }
            }
        }
        compose.waitUntil(5_000) {
            compose.onNodeWithTag("proposal-headline").let { runCatching { it.assertExists() }.isSuccess }
        }
    }

    @Test fun counterOfferShowsTimelineAndActions() {
        val db = FakeBackend(asRealtor = false)
        show(db, 302)
        compose.onNodeWithText("O proprietário fez uma contraproposta").assertIsDisplayed()
        compose.onNodeWithText("Contraproposta do proprietário").assertExists()
        compose.onNodeWithText("Sua proposta").assertExists()
        compose.onNodeWithText("Aceitar").assertIsDisplayed()
        compose.onNodeWithText("Contrapropor").assertIsDisplayed()

        compose.onNodeWithText("Aceitar").performClick()
        compose.onNodeWithText("Aceitar contraproposta?").assertIsDisplayed()
    }

    @Test fun waitingProposalHasNoActions() {
        val db = FakeBackend(asRealtor = false)
        show(db, 301)
        compose.onNodeWithText("Aguardando resposta do proprietário").assertIsDisplayed()
        compose.onNodeWithText("Contrapropor").assertDoesNotExist()
        assertEquals(ApplicationStatus.UNDER_REVIEW, db.applicationRows.single { it.id == 301L }.status)
    }
}
