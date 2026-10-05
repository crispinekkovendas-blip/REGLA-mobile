package br.com.imoveisregla.realtor.feature.proposals

import br.com.imoveisregla.core.data.fake.FakeBackend
import br.com.imoveisregla.core.model.ApplicationStatus
import br.com.imoveisregla.core.model.InquiryStage
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

    @Test
    fun loadsSeededApplicationWithProfile() {
        val s = vm().state.value
        assertFalse(s.loading)
        assertNull(s.error)
        assertEquals("Mariana Souza", s.application!!.profile!!.fullName)
        assertEquals(ApplicationStatus.UNDER_REVIEW, s.application!!.status)
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
    fun approveUpdatesRowAndClosesLinkedLead() {
        val vm = vm()
        assertNull(vm.review(ApplicationStatus.APPROVED, null))
        assertEquals(ApplicationStatus.APPROVED, row().status)
        assertEquals(InquiryStage.CLOSED_WON, fake.inquiryRows.first { it.id == 105L }.stage)
        val s = vm.state.value
        assertEquals(ApplicationStatus.APPROVED, s.application!!.status)
        assertEquals("Proposta aprovada", s.message)
        assertFalse(s.canAct)
        assertFalse(s.submitting)
    }

    @Test
    fun rejectRequiresNote() {
        val vm = vm()
        assertNotNull(vm.review(ApplicationStatus.REJECTED, null))
        assertNotNull(vm.review(ApplicationStatus.REJECTED, "   "))
        assertEquals(ApplicationStatus.UNDER_REVIEW, row().status)
        assertEquals("Escreva uma observação para o cliente", vm.state.value.message)

        assertNull(vm.review(ApplicationStatus.REJECTED, "  Renda insuficiente "))
        assertEquals(ApplicationStatus.REJECTED, row().status)
        assertEquals("Renda insuficiente", row().reviewerNote)
        // the linked lead is not closed on rejection
        assertEquals(InquiryStage.OFFER, fake.inquiryRows.first { it.id == 105L }.stage)
    }

    @Test
    fun docsRequestRequiresNote() {
        val vm = vm()
        assertNotNull(vm.review(ApplicationStatus.DOCS_REQUESTED, ""))
        assertEquals(ApplicationStatus.UNDER_REVIEW, row().status)

        assertNull(vm.review(ApplicationStatus.DOCS_REQUESTED, "Envie o comprovante de renda"))
        assertEquals(ApplicationStatus.DOCS_REQUESTED, row().status)
        assertEquals("Envie o comprovante de renda", row().reviewerNote)
        assertTrue(vm.state.value.canAct) // still open
    }

    @Test
    fun actionsBlockedWhenNotOpen() {
        val vm = vm()
        vm.review(ApplicationStatus.APPROVED, null)
        vm.consumeMessage()

        val err = vm.review(ApplicationStatus.REJECTED, "Mudei de ideia")
        assertEquals("Esta proposta já foi finalizada", err)
        assertEquals(ApplicationStatus.APPROVED, row().status)
        assertNotNull(vm.review(ApplicationStatus.UNDER_REVIEW, null))
        assertEquals(ApplicationStatus.APPROVED, row().status)
    }

    @Test
    fun withdrawnApplicationIsReadOnly() {
        val i = fake.applicationRows.indexOfFirst { it.id == 301L }
        fake.applicationRows[i] = fake.applicationRows[i].copy(status = ApplicationStatus.WITHDRAWN)
        val vm = vm()
        assertFalse(vm.state.value.canAct)
        assertNotNull(vm.review(ApplicationStatus.APPROVED, null))
        assertEquals(ApplicationStatus.WITHDRAWN, row().status)
    }

    @Test
    fun listViewModelCountsAndFilters() {
        val list = ProposalsViewModel(fake.applications)
        val s = list.state.value
        assertEquals(1, s.counts[null])
        assertEquals(1, s.counts[ApplicationStatus.UNDER_REVIEW])
        assertEquals("Mariana Souza", s.visible.single().profile?.fullName)
        list.setFilter(ApplicationStatus.APPROVED)
        assertTrue(list.state.value.visible.isEmpty())
        list.setFilter(null)
        assertEquals(1, list.state.value.visible.size)
    }
}
