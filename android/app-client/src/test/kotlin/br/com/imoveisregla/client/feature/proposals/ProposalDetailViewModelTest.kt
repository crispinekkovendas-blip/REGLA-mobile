package br.com.imoveisregla.client.feature.proposals

import br.com.imoveisregla.core.data.fake.FakeBackend
import br.com.imoveisregla.core.model.ApplicationStatus
import br.com.imoveisregla.core.model.DocumentKind
import br.com.imoveisregla.core.model.Party
import br.com.imoveisregla.core.model.UploadFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProposalDetailViewModelTest {

    @Before fun setUp() { Dispatchers.setMain(UnconfinedTestDispatcher()) }
    @After fun tearDown() { Dispatchers.resetMain() }

    private fun pdf(name: String) = UploadFile(name, "application/pdf", ByteArray(64) { 1 })

    @Test fun `waiting on the owner offers no actions`() {
        val vm = ProposalDetailViewModel(301, FakeBackend(asRealtor = false))
        val s = vm.state.value
        assertEquals(ClientAction.WAIT, s.action)
        assertEquals("Aguardando resposta do proprietário", clientHeadline(s.app!!))
    }

    @Test fun `client counters the owner counter-offer`() {
        val db = FakeBackend(asRealtor = false)
        val vm = ProposalDetailViewModel(302, db)
        assertEquals(ClientAction.RESPOND_OFFER, vm.state.value.action)
        assertEquals("O proprietário fez uma contraproposta", clientHeadline(vm.state.value.app!!))

        vm.openCounter()
        assertEquals("3800", vm.state.value.counterDigits)
        vm.sendCounter() // same as current offer → rejected locally
        assertEquals("Informe um valor diferente da oferta atual", vm.state.value.counterError)

        vm.updateCounter(digits = "3700", message = "Meio termo?")
        vm.sendCounter()
        val app = vm.state.value.app!!
        assertEquals(ApplicationStatus.NEGOTIATING, app.status)
        assertEquals(Party.REALTOR, app.awaiting)
        assertEquals(3_700L, app.currentPrice)
        assertEquals(ClientAction.WAIT, vm.state.value.action)
        assertEquals("Contraproposta enviada", vm.state.value.message)
    }

    @Test fun `accepting then sending documents moves to review`() {
        val db = FakeBackend(asRealtor = false)
        val vm = ProposalDetailViewModel(302, db)
        vm.ask(ConfirmKind.ACCEPT)
        vm.confirm()
        var s = vm.state.value
        assertEquals(ApplicationStatus.ACCEPTED, s.app?.status)
        assertEquals(3_800L, s.app?.agreedPrice)
        assertEquals(ClientAction.SEND_DOCUMENTS, s.action)

        // can't conclude while required documents are missing
        vm.askSendDocuments()
        assertNull(vm.state.value.confirm)
        assertEquals(3, vm.state.value.missingDocuments.size)

        vm.upload(DocumentKind.RG_CNH, pdf("rg.pdf"))
        vm.upload(DocumentKind.COMPROVANTE_RENDA, pdf("holerite.pdf"))
        vm.upload(DocumentKind.COMPROVANTE_RESIDENCIA, pdf("luz.pdf"))
        assertTrue(vm.state.value.missingDocuments.isEmpty())
        assertTrue(db.documentRows.filter { it.applicationId == 302L }.size == 3)

        vm.askSendDocuments()
        assertEquals(ConfirmKind.SEND_DOCS, vm.state.value.confirm)
        vm.confirm()
        s = vm.state.value
        assertEquals(ApplicationStatus.DOCS_REVIEW, s.app?.status)
        assertEquals(ClientAction.WAIT, s.action)
    }

    @Test fun `declining withdraws`() {
        val db = FakeBackend(asRealtor = false)
        val vm = ProposalDetailViewModel(302, db)
        vm.ask(ConfirmKind.DECLINE)
        vm.confirm()
        assertEquals(ApplicationStatus.WITHDRAWN, vm.state.value.app?.status)
        assertEquals(ClientAction.CLOSED, vm.state.value.action)
    }

    @Test fun `only documents of this proposta are listed`() {
        val db = FakeBackend(asRealtor = false)
        kotlinx.coroutines.runBlocking {
            db.documents.upload(DocumentKind.CPF, pdf("cpf.pdf"), applicationId = null)
            db.documents.upload(DocumentKind.RG_CNH, pdf("rg.pdf"), applicationId = 302)
        }
        val vm = ProposalDetailViewModel(302, db)
        assertEquals(listOf(DocumentKind.RG_CNH), vm.state.value.documents.map { it.kind })
    }
}
