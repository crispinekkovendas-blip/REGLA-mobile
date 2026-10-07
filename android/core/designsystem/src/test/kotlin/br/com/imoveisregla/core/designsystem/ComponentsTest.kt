package br.com.imoveisregla.core.designsystem

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import br.com.imoveisregla.core.model.Affordability
import br.com.imoveisregla.core.model.ApplicationStatus
import br.com.imoveisregla.core.model.Currency
import br.com.imoveisregla.core.model.InquiryStage
import br.com.imoveisregla.core.model.Listing
import br.com.imoveisregla.core.model.ListingStatus
import br.com.imoveisregla.core.model.ListingType
import br.com.imoveisregla.core.model.Priority
import br.com.imoveisregla.core.model.ShowingStatus
import br.com.imoveisregla.core.model.formatArea
import br.com.imoveisregla.core.model.formatPrice
import br.com.imoveisregla.core.model.label
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ComponentsTest {
    @get:Rule val rule = createComposeRule()

    private val rental = Listing(
        id = 42, slug = "apto-pinheiros", title = "Apartamento em Pinheiros", city = "São Paulo", country = "BR",
        neighborhood = "Pinheiros", type = ListingType.APARTMENT, price = 4_500, currency = Currency.BRL,
        beds = 2, baths = 1, areaM2 = 68, tags = listOf("Aluguel"),
    )
    private val sale = rental.copy(id = 7, price = 1_850_000, tags = emptyList(), beds = 3, baths = 2, areaM2 = 120)

    @Test fun listingCard_showsRentalPriceRefAndSpecs() {
        rule.setContent {
            ReglaTheme { ListingCard(rental, photoUrl = null, isFavorite = false, onToggleFavorite = {}, onClick = {}) }
        }
        rule.onNodeWithText(formatPrice(4_500, Currency.BRL)).assertExists()
        rule.onNodeWithText("/mês").assertExists()
        rule.onNodeWithText("RG-0042").assertExists()
        rule.onNodeWithText("2 quartos").assertExists()
        rule.onNodeWithText("1 banheiro").assertExists()
        rule.onNodeWithText(formatArea(68)).assertExists()
        rule.onNodeWithText("Pinheiros · São Paulo").assertExists()
        rule.onNodeWithText("Apartamento em Pinheiros").assertExists()
        rule.onNodeWithText("Aluguel").assertExists()
    }

    @Test fun listingCard_saleHasNoMonthlySuffix() {
        rule.setContent {
            ReglaTheme { ListingCard(sale, photoUrl = null, isFavorite = true, onToggleFavorite = null, onClick = {}) }
        }
        rule.onNodeWithText("R$ 1.850.000").assertExists()
        rule.onNodeWithText("/mês").assertDoesNotExist()
        rule.onNodeWithText("RG-0007").assertExists()
        rule.onNodeWithText("3 quartos").assertExists()
        // heart hidden when no callback
        rule.onNodeWithContentDescription("Remover dos favoritos").assertDoesNotExist()
    }

    @Test fun listingCard_favoriteToggleAndClickCallbacks() {
        var toggles = 0
        var clicks = 0
        rule.setContent {
            var fav by mutableStateOf(false)
            ReglaTheme {
                ListingCard(rental, null, isFavorite = fav, onToggleFavorite = { toggles++; fav = !fav }, onClick = { clicks++ })
            }
        }
        rule.onNodeWithContentDescription("Adicionar aos favoritos").performClick()
        assertEquals(1, toggles)
        assertEquals(0, clicks)
        rule.onNodeWithContentDescription("Remover dos favoritos").assertExists().performClick()
        assertEquals(2, toggles)
        rule.onNodeWithText("Apartamento em Pinheiros").performClick()
        assertEquals(1, clicks)
    }

    @Test fun listingPriceText_formats() {
        assertEquals(formatPrice(4_500, Currency.BRL) + "/mês", listingPriceText(rental))
        assertEquals("R$ 1.850.000", listingPriceText(sale))
    }

    @Test fun statusPills_renderPortugueseLabels() {
        rule.setContent {
            ReglaTheme {
                androidx.compose.foundation.layout.Column {
                    ApplicationStatusPill(ApplicationStatus.UNDER_REVIEW)
                    ShowingStatusPill(ShowingStatus.NO_SHOW)
                    ListingStatusPill(ListingStatus.LIVE)
                    StagePill(InquiryStage.QUALIFIED)
                    PriorityBadge(Priority.HIGH)
                    AffordabilityBadge(Affordability.TIGHT)
                }
            }
        }
        rule.onNodeWithText("Em análise").assertExists()
        rule.onNodeWithText("Não compareceu").assertExists()
        rule.onNodeWithText("Ativo").assertExists()
        rule.onNodeWithText("Qualificado").assertExists()
        rule.onNodeWithText("Alta").assertExists()
        rule.onNodeWithText("Orçamento apertado").assertExists()
    }

    @Test fun pillColorMappings() {
        assertEquals(PillPalette.Ok, ApplicationStatus.APPROVED.pillColors())
        assertEquals(PillPalette.Danger, ApplicationStatus.REJECTED.pillColors())
        assertEquals(PillPalette.Warn, ApplicationStatus.UNDER_REVIEW.pillColors())
        assertEquals(PillPalette.Brand, ApplicationStatus.DOCS_REQUESTED.pillColors())
        assertEquals(PillPalette.Neutral, ApplicationStatus.WITHDRAWN.pillColors())
        assertEquals(PillPalette.Info, ApplicationStatus.SUBMITTED.pillColors())
        assertEquals(PillPalette.Ok, ShowingStatus.CONFIRMED.pillColors())
        assertEquals(PillPalette.Danger, ShowingStatus.NO_SHOW.pillColors())
        assertEquals(PillPalette.Neutral, ShowingStatus.CANCELLED.pillColors())
        assertEquals(PillPalette.Ok, ListingStatus.LIVE.pillColors())
        assertEquals(PillPalette.Neutral, ListingStatus.DRAFT.pillColors())
        assertEquals(PillPalette.Ok, InquiryStage.CLOSED_WON.pillColors())
        assertEquals(PillPalette.Brand, InquiryStage.INBOX.pillColors())
        assertEquals(PillPalette.Danger, Priority.HIGH.pillColors())
        assertEquals(PillPalette.Neutral, Priority.LOW.pillColors())
        assertEquals(PillPalette.Ok, Affordability.OK.pillColors())
        assertEquals(PillPalette.Warn, Affordability.TIGHT.pillColors())
        assertEquals(PillPalette.Danger, Affordability.OVER.pillColors())
        assertEquals(PillPalette.Neutral, Affordability.UNKNOWN.pillColors())
        // every application status is visually distinct
        assertEquals(ApplicationStatus.entries.size, ApplicationStatus.entries.map { it.pillColors() }.toSet().size)
        assertEquals(ShowingStatus.entries.size, ShowingStatus.entries.map { it.pillColors() }.toSet().size)
        assertEquals(InquiryStage.entries.size, InquiryStage.entries.map { it.pillColors() }.toSet().size)
    }

    @Test fun stepIndicator_marksDoneAndCurrent() {
        rule.setContent {
            ReglaTheme { StepIndicator(listOf("Dados", "Documentos", "Revisão"), current = 1) }
        }
        rule.onNodeWithText("Dados").assertExists()
        rule.onNodeWithText("Documentos").assertExists()
        rule.onNodeWithText("Revisão").assertExists()
        rule.onNodeWithContentDescription("Etapa 2 de 3: Documentos").assertExists()
        rule.onAllNodesWithContentDescription("Etapa concluída").assertCountEquals(1)
        rule.onNodeWithText("3").assertExists()
    }

    @Test fun stepIndicator_clampsOutOfRange() {
        rule.setContent { ReglaTheme { StepIndicator(listOf("A", "B"), current = 9) } }
        rule.onNodeWithContentDescription("Etapa 2 de 2: B").assertExists()
        rule.onAllNodesWithContentDescription("Etapa concluída").assertCountEquals(1)
    }

    @Test fun applicationTimeline_steps() {
        assertEquals(
            listOf("Enviada", "Negociação", "Aceita", "Documentos", "Contrato") to 0,
            applicationTimeline(ApplicationStatus.SUBMITTED),
        )
        assertEquals(1, applicationTimeline(ApplicationStatus.NEGOTIATING).second)
        assertEquals(2, applicationTimeline(ApplicationStatus.ACCEPTED).second)
        assertEquals(3, applicationTimeline(ApplicationStatus.DOCS_REQUESTED).second)
        assertEquals("Documentos pendentes", applicationTimeline(ApplicationStatus.DOCS_REQUESTED).first[3])
        assertEquals("Documentos em análise", applicationTimeline(ApplicationStatus.DOCS_REVIEW).first[3])
        assertEquals(4, applicationTimeline(ApplicationStatus.APPROVED).second)
        assertEquals("Aprovada", applicationTimeline(ApplicationStatus.APPROVED).first[4])
        assertEquals("Recusada", applicationTimeline(ApplicationStatus.REJECTED).first[4])
    }

    @Test fun statusTimeline_rendersSteps() {
        rule.setContent { ReglaTheme { ApplicationStatusTimeline(ApplicationStatus.UNDER_REVIEW) } }
        rule.onNodeWithText("Enviada").assertExists()
        rule.onNodeWithText("Negociação").assertExists()
        rule.onNodeWithText("Contrato").assertExists()
    }

    @Test fun initials_rules() {
        assertEquals("AS", initials("Ana Paula Souza"))
        assertEquals("M", initials("maria"))
        assertEquals("JS", initials("  joão   silva "))
        assertEquals("?", initials("   "))
    }

    @Test fun avatar_showsInitials() {
        rule.setContent { ReglaTheme { Avatar("Carlos Mendes") } }
        rule.onNodeWithText("CM").assertExists()
    }

    @Test fun parseMoneyDigits_rules() {
        assertEquals(1_850_000L, parseMoneyDigits("R$ 1.850.000"))
        assertEquals(45_001L, parseMoneyDigits("R$ 4.5001"))
        assertNull(parseMoneyDigits("R$ "))
        assertNull(parseMoneyDigits("000"))
        assertEquals(999_999_999_999L, parseMoneyDigits("9999999999999999"))
    }

    @Test fun moneyField_formatsTypedDigits() {
        var value: Long? = null
        rule.setContent {
            var v by mutableStateOf<Long?>(null)
            ReglaTheme { MoneyField(v, { v = it; value = it }, label = "Valor") }
        }
        rule.onNodeWithText("Valor").performTextInput("4500")
        assertEquals(4_500L, value)
        rule.onNodeWithText(formatPrice(4_500, Currency.BRL)).assertExists()
        rule.onNodeWithText(formatPrice(4_500, Currency.BRL)).performTextReplacement("abc")
        assertNull(value)
    }

    @Test fun filterChipGroup_selects() {
        var picked: ListingType? = null
        rule.setContent {
            var sel by mutableStateOf<ListingType?>(ListingType.HOUSE)
            ReglaTheme {
                FilterChipGroup(ListingType.entries, sel, { it.label }, { sel = it; picked = it })
            }
        }
        rule.onNodeWithText("Apartamento").performClick()
        assertEquals(ListingType.APARTMENT, picked)
    }

    @Test fun confirmDialog_callbacks() {
        var confirmed = false
        var dismissed = false
        rule.setContent {
            ReglaTheme {
                ConfirmDialog("Cancelar visita?", "Essa ação não pode ser desfeita.", { confirmed = true }, { dismissed = true }, confirmText = "Sim, cancelar", dismissText = "Voltar", destructive = true)
            }
        }
        rule.onNodeWithText("Cancelar visita?").assertExists()
        rule.onNodeWithText("Sim, cancelar").performClick()
        assertTrue(confirmed)
        rule.onNodeWithText("Voltar").performClick()
        assertTrue(dismissed)
    }

    @Test fun legacyComponents_render() {
        rule.setContent {
            ReglaTheme {
                androidx.compose.foundation.layout.Column {
                    StatTile("Novos leads", "12")
                    InfoRow("Renda", "R$ 8.000")
                    SectionTitle("Destaques")
                    ChipRow(listOf("Varanda", "Pet friendly"))
                    DemoBanner()
                    ReglaTopBar("Detalhes", onBack = {})
                }
            }
        }
        rule.onNodeWithText("12").assertExists()
        rule.onNodeWithText("Novos leads").assertExists()
        rule.onNodeWithText("R$ 8.000").assertExists()
        rule.onNodeWithText("Destaques").assertExists()
        rule.onNodeWithText("Pet friendly").assertExists()
        rule.onNodeWithText("Modo demonstração · dados de exemplo").assertExists()
        rule.onNodeWithContentDescription("Voltar").assertExists()
    }

    @Test fun errorAndEmptyStates() {
        var retried = false
        rule.setContent { ReglaTheme { ErrorState("Sem conexão", onRetry = { retried = true }) } }
        rule.onNodeWithText("Algo deu errado").assertExists()
        rule.onNodeWithText("Sem conexão").assertExists()
        rule.onNodeWithText("Tentar novamente").performClick()
        assertTrue(retried)
    }

    @Test fun buttonLoadingDisablesClick() {
        var clicks = 0
        rule.setContent {
            ReglaTheme {
                androidx.compose.foundation.layout.Column {
                    ReglaButton("Enviar", { clicks++ })
                    ReglaButton("Salvar", { clicks += 10 }, loading = true)
                }
            }
        }
        rule.onNodeWithText("Enviar").performClick()
        rule.onNodeWithText("Salvar").assertDoesNotExist()
        assertEquals(1, clicks)
    }
}
