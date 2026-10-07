package br.com.imoveisregla.realtor.feature.proposals

import br.com.imoveisregla.core.data.fake.FakeBackend
import br.com.imoveisregla.core.model.ApplicationStatus
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
class ApplicationDetailViewModelTest {

    private lateinit var fake: FakeBackend

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        fake = FakeBackend(asRealtor = true)
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun vm(id: Long = 301) = ApplicationDetailViewModel(fake.applications, fake.documents, id)
    private fun row(id: Long = 301) = fake.applicationRows.first { it.id == id }

    /** Puts 301 in the documents-review phase (client accepted + sent documents). */
    private fun toDocsReview() {
        val i = fake.applicationRows.indexOfFirst { it.id == 301L }
        fake.applicationRows[i] = fake.applicationRows[i].copy(
            status = ApplicationStatus.DOCS_REVIEW, awaiting = Party.REALTOR, agreedPrice = 4_600,
        )
    }

    @Test
    fun loadsSeededApplicationWithProfileAndOffers() {
        val s = vm().state.value
        assertFalse(s.loading)
        assertNull(s.error)
        assertEquals("Mariana Souza", s.application!!.profile!!.fullName)
        assertEquals(ApplicationStatus.UNDER_REVIEW, s.application!!.status)
        assertEquals(1, s.application!!.offers.size)
        assertEquals(RealtorAction.RESPOND_OFFER, s.action)
        assertTrue(s.canAct)
    }

    @Test
    fun missingApplicationShowsError() {
        val s = vm(999).state.value
        assertFalse(s.loading)
        assertNull(s.application)
        assertNotNull(s.error)
    }

    @Test
    fun acceptLocksPriceAndWaitsForDocuments() {
        val vm = vm()
        vm.accept()
        val s = vm.state.value
        assertEquals(ApplicationStatus.ACCEPTED, s.application!!.status)
        assertEquals(4_600L, s.application!!.agreedPrice)
        assertEquals(RealtorAction.WAIT_CLIENT, s.action)
        assertEquals("Proposta aceita · o cliente vai enviar os documentos", s.message)
        // the lead stays an open offer until final approval
        assertEquals(InquiryStage.OFFER, fake.inquiryRows.first { it.id == 105L }.stage)
    }

    @Test
    fun counterValidatesAndHandsTurnToClient() {
        val vm = vm()
        assertEquals("Informe um valor", vm.counter(null, null))
        assertEquals("Informe um valor diferente da oferta atual", vm.counter(4_600, null))
        assertNull(vm.counter(4_750, "  Proprietário pede 4.750 "))
        val app = vm.state.value.application!!
        assertEquals(ApplicationStatus.NEGOTIATING, app.status)
        assertEquals(Party.CLIENT, app.awaiting)
        assertEquals(4_750L, app.currentPrice)
        assertEquals("Proprietário pede 4.750", app.latestOffer?.message)
        assertEquals(RealtorAction.WAIT_CLIENT, vm.state.value.action)
    }

    @Test
    fun declineRequiresNote() {
        val vm = vm()
        assertNotNull(vm.decline("   "))
        assertEquals(ApplicationStatus.UNDER_REVIEW, row().status)
        assertNull(vm.decline("  Renda insuficiente "))
        assertEquals(ApplicationStatus.REJECTED, row().status)
        assertEquals("Renda insuficiente", row().reviewerNote)
        assertEquals(InquiryStage.OFFER, fake.inquiryRows.first { it.id == 105L }.stage)
        assertFalse(vm.state.value.canAct)
    }

    @Test
    fun approveOnlyAfterDocuments() {
        val vm = vm()
        assertEquals("Ação indisponível nesta etapa", vm.review(ApplicationStatus.APPROVED, null))
        assertEquals(ApplicationStatus.UNDER_REVIEW, row().status)

        toDocsReview()
        vm.load()
        assertEquals(RealtorAction.REVIEW_DOCS, vm.state.value.action)
        assertNull(vm.review(ApplicationStatus.APPROVED, null))
        assertEquals(ApplicationStatus.APPROVED, row().status)
        assertEquals(InquiryStage.CLOSED_WON, fake.inquiryRows.first { it.id == 105L }.stage)
        assertEquals("Proposta aprovada", vm.state.value.message)
        assertFalse(vm.state.value.canAct)
    }

    @Test
    fun correctionRequiresNoteAndReturnsTurnToClient() {
        toDocsReview()
        val vm = vm()
        assertNotNull(vm.review(ApplicationStatus.DOCS_REQUESTED, ""))
        assertNull(vm.review(ApplicationStatus.DOCS_REQUESTED, "Envie o comprovante de renda"))
        assertEquals(ApplicationStatus.DOCS_REQUESTED, row().status)
        assertEquals(Party.CLIENT, row().awaiting)
        assertEquals("Envie o comprovante de renda", row().reviewerNote)
        assertEquals(RealtorAction.WAIT_CLIENT, vm.state.value.action)
    }

    @Test
    fun withdrawnApplicationIsReadOnly() {
        val i = fake.applicationRows.indexOfFirst { it.id == 301L }
        fake.applicationRows[i] = fake.applicationRows[i].copy(status = ApplicationStatus.WITHDRAWN, awaiting = null)
        val vm = vm()
        assertFalse(vm.state.value.canAct)
        assertEquals(RealtorAction.CLOSED, vm.state.value.action)
        assertNotNull(vm.review(ApplicationStatus.APPROVED, null))
        assertEquals(ApplicationStatus.WITHDRAWN, row().status)
    }

    @Test
    fun listViewModelCountsAndFilters() {
        val list = ProposalsViewModel(fake.applications)
        val s = list.state.value
        assertEquals(2, s.counts[ProposalFilter.ALL])
        assertEquals(1, s.counts[ProposalFilter.YOUR_TURN])
        assertEquals(2, s.counts[ProposalFilter.NEGOTIATION])
        list.setFilter(ProposalFilter.YOUR_TURN)
        assertEquals(listOf(301L), list.state.value.visible.map { it.id })
        list.setFilter(ProposalFilter.APPROVED)
        assertTrue(list.state.value.visible.isEmpty())
        list.setFilter(ProposalFilter.ALL)
        assertEquals(2, list.state.value.visible.size)
    }
}
