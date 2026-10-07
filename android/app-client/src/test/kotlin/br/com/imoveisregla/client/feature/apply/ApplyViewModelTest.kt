package br.com.imoveisregla.client.feature.apply

import br.com.imoveisregla.core.data.fake.FakeBackend
import br.com.imoveisregla.core.model.ApplicationIntent
import br.com.imoveisregla.core.model.EmploymentType
import br.com.imoveisregla.core.model.GuaranteeType
import br.com.imoveisregla.core.model.InquiryStage
import br.com.imoveisregla.core.model.Party
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ApplyViewModelTest {

    @Before fun setUp() { Dispatchers.setMain(UnconfinedTestDispatcher()) }
    @After fun tearDown() { Dispatchers.resetMain() }

    @Test fun `prefills step 1 from the saved profile`() {
        val vm = ApplyViewModel(2, FakeBackend(asRealtor = false))
        val s = vm.state.value
        assertFalse(s.loading)
        assertEquals(WizardStep.PROFILE, s.step)
        assertEquals("Mariana Souza", s.profile.fullName)
        assertEquals("52998224725", s.profile.cpfDigits)
        assertEquals("11987654321", s.profile.phoneDigits)
        assertEquals("18000", s.profile.incomeDigits)
        assertEquals(EmploymentType.CLT, s.profile.employmentType)
    }

    @Test fun `step 1 blocks on invalid CPF`() {
        val db = FakeBackend(asRealtor = false)
        val vm = ApplyViewModel(2, db)
        vm.updateProfile { it.copy(cpfDigits = "11111111111", fullName = "Outro Nome") }
        vm.next()
        val s = vm.state.value
        assertEquals(WizardStep.PROFILE, s.step)
        assertEquals("CPF inválido", s.profileErrors["cpf"])
        assertEquals("Mariana Souza", db.profileRows["client-1"]?.fullName) // not saved
    }

    @Test fun `step 1 needs CPF and income, but not the employment type`() {
        val vm = ApplyViewModel(2, FakeBackend(asRealtor = false))
        vm.updateProfile { it.copy(incomeDigits = "", cpfDigits = "", employmentType = null) }
        vm.next()
        val s = vm.state.value
        assertEquals(WizardStep.PROFILE, s.step)
        assertEquals(setOf("monthlyIncome", "cpf"), s.profileErrors.keys)

        vm.updateProfile { it.copy(incomeDigits = "9000", cpfDigits = "52998224725") }
        vm.next()
        assertEquals(WizardStep.OFFER, vm.state.value.step)
    }

    @Test fun `step 1 saves a new profile prefilled with the session email`() {
        val db = FakeBackend(asRealtor = false)
        db.profileRows.remove("client-1")
        val vm = ApplyViewModel(2, db)
        assertEquals("cliente@exemplo.com", vm.state.value.profile.email)
        vm.updateProfile {
            it.copy(
                fullName = "João da Silva", phoneDigits = "11912345678", cpfDigits = "52998224725",
                employmentType = EmploymentType.AUTONOMO, incomeDigits = "12000", residents = 3,
            )
        }
        vm.next()
        assertEquals(WizardStep.OFFER, vm.state.value.step)
        val saved = db.profileRows["client-1"]
        assertNotNull(saved)
        assertEquals("João da Silva", saved!!.fullName)
        assertEquals("529.982.247-25", saved.cpf)
        assertEquals("(11) 91234-5678", saved.phone)
        assertEquals(12_000L, saved.monthlyIncome)
        assertEquals(3, saved.residents)
    }

    @Test fun `default intent follows the listing kind`() {
        val db = FakeBackend(asRealtor = false)
        val rent = ApplyViewModel(1, db).state.value.offer
        assertEquals(ApplicationIntent.RENT, rent.intent)
        assertEquals(4_800L, rent.offeredPrice)
        val buy = ApplyViewModel(4, db).state.value.offer
        assertEquals(ApplicationIntent.BUY, buy.intent)
        assertEquals(3_950_000L, buy.offeredPrice)
    }

    @Test fun `step 2 validates price and guarantee`() {
        val vm = ApplyViewModel(2, FakeBackend(asRealtor = false))
        vm.next() // profile ok
        assertEquals(WizardStep.OFFER, vm.state.value.step)
        vm.updateOffer { it.copy(priceDigits = "") }
        vm.next()
        var s = vm.state.value
        assertEquals(WizardStep.OFFER, s.step)
        assertEquals("Informe um valor", s.offerErrors["offeredPrice"])
        assertEquals("Escolha uma garantia", s.offerErrors["guaranteeType"])

        vm.updateOffer { it.copy(priceDigits = "3000", guaranteeType = GuaranteeType.CAUCAO, message = "x".repeat(2500)) }
        s = vm.state.value
        assertEquals(MAX_MESSAGE_LENGTH, s.offer.message.length)
        vm.next()
        assertEquals(WizardStep.REVIEW, vm.state.value.step)
    }

    @Test fun `buy offers do not need a guarantee`() {
        val vm = ApplyViewModel(4, FakeBackend(asRealtor = false))
        vm.next()
        vm.next()
        assertEquals(WizardStep.REVIEW, vm.state.value.step)
        assertNull(vm.state.value.offer.toInput(4).guaranteeType)
    }

    @Test fun `wizard has no documents step`() {
        assertEquals(listOf(WizardStep.PROFILE, WizardStep.OFFER, WizardStep.REVIEW), WizardStep.entries)
    }

    @Test fun `submit sends the proposta without documents and opens the negotiation`() {
        val db = FakeBackend(asRealtor = false)
        val vm = ApplyViewModel(2, db)
        vm.next()
        vm.updateOffer { it.copy(priceDigits = "3100", guaranteeType = GuaranteeType.FIADOR, moveInDate = "2026-11-01", message = "Posso mudar em novembro") }
        vm.next()
        assertEquals(WizardStep.REVIEW, vm.state.value.step)

        // declaration is mandatory
        vm.next()
        assertTrue(vm.state.value.declarationError)
        assertNull(vm.state.value.submitted)
        assertTrue(db.applicationRows.none { it.listingId == 2L })

        vm.setDeclared(true)
        vm.next()
        val app = vm.state.value.submitted
        assertNotNull(app)
        app!!
        assertEquals(2L, app.listingId)
        assertEquals(3_100L, app.offeredPrice)
        assertEquals(GuaranteeType.FIADOR, app.guaranteeType)
        assertEquals("2026-11-01", app.moveInDate)
        assertEquals(ApplicationIntent.RENT, app.intent)
        assertEquals(Party.REALTOR, app.awaiting)
        assertEquals(listOf(3_100L), app.offers.map { it.price })
        assertTrue(db.applicationRows.any { it.id == app.id })
        assertTrue(db.documentRows.none { it.applicationId == app.id })

        val inquiry = db.inquiryRows.firstOrNull { it.id == app.inquiryId }
        assertNotNull(inquiry)
        assertEquals(InquiryStage.OFFER, inquiry!!.stage)
        assertEquals(2L, inquiry.propertyId)
    }

    @Test fun `warns when an open application already exists for the listing`() {
        val db = FakeBackend(asRealtor = false)
        assertEquals(301L, ApplyViewModel(1, db).state.value.existingOpen?.id)
        assertNull(ApplyViewModel(2, db).state.value.existingOpen)
    }

    @Test fun `back walks the steps`() {
        val vm = ApplyViewModel(2, FakeBackend(asRealtor = false))
        assertFalse(vm.back())
        vm.next()
        assertEquals(WizardStep.OFFER, vm.state.value.step)
        assertTrue(vm.back())
        assertEquals(WizardStep.PROFILE, vm.state.value.step)
    }

    @Test fun `signed out users cannot start the wizard`() {
        val vm = ApplyViewModel(2, FakeBackend(asRealtor = false, signedIn = false))
        assertTrue(vm.state.value.signedOut)
    }

    @Test fun `price difference helper`() {
        assertEquals(-4, priceDifferencePercent(4_600, 4_800))
        assertEquals("Diferença do anúncio: -4%", priceDifferenceLabel(4_600, 4_800))
        assertEquals("Diferença do anúncio: +10%", priceDifferenceLabel(5_280, 4_800))
        assertEquals("Mesmo valor do anúncio", priceDifferenceLabel(4_800, 4_800))
        assertNull(priceDifferenceLabel(0, 4_800))
    }
}
