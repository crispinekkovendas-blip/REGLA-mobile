package br.com.imoveisregla.realtor.feature.leads

import br.com.imoveisregla.core.data.fake.FakeBackend
import br.com.imoveisregla.core.model.InquiryStage
import br.com.imoveisregla.core.model.Priority
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LeadsViewModelTest {
    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @After fun tearDown() = Dispatchers.resetMain()

    @Test fun loadsSeedAndCounts() {
        val vm = LeadsViewModel(FakeBackend(asRealtor = true).leads)
        val s = vm.state.value
        assertFalse(s.loading)
        assertEquals(6, s.all.size)
        assertEquals(6, s.visible.size)
        assertEquals(2, s.counts[InquiryStage.INBOX])
        assertEquals(1, s.counts[InquiryStage.QUALIFIED])
        assertEquals(1, s.counts[InquiryStage.SHOWING])
        assertEquals(1, s.counts[InquiryStage.OFFER])
        assertEquals(1, s.counts[InquiryStage.CLOSED_WON])
        assertEquals(0, s.counts[InquiryStage.CLOSED_LOST])
    }

    @Test fun stageFilter() {
        val vm = LeadsViewModel(FakeBackend(asRealtor = true).leads)
        vm.setStage(InquiryStage.INBOX)
        assertEquals(setOf("Carlos Lima", "Fernanda Alves"), vm.state.value.visible.map { it.name }.toSet())
        // counts are independent of the active filter
        assertEquals(6, vm.state.value.all.size)
        vm.setStage(InquiryStage.CLOSED_LOST)
        assertTrue(vm.state.value.visible.isEmpty())
        vm.setStage(null)
        assertEquals(6, vm.state.value.visible.size)
    }

    @Test fun searchByNameEmailAndMessage() {
        val vm = LeadsViewModel(FakeBackend(asRealtor = true).leads)
        vm.setQuery("carlos")
        assertEquals(listOf(101L), vm.state.value.visible.map { it.id })
        vm.setQuery("JULIANA@exemplo")
        assertEquals(listOf(104L), vm.state.value.visible.map { it.id })
        vm.setQuery("pets")
        assertEquals(listOf(102L), vm.state.value.visible.map { it.id })
        vm.setStage(InquiryStage.SHOWING)
        assertTrue(vm.state.value.visible.isEmpty())
    }

    @Test fun refreshPicksUpNewRows() {
        val backend = FakeBackend(asRealtor = true)
        val vm = LeadsViewModel(backend.leads)
        backend.inquiryRows.removeAll { it.id == 106L }
        vm.refresh()
        assertFalse(vm.state.value.refreshing)
        assertEquals(5, vm.state.value.all.size)
    }

    @Test fun emptyBackend() {
        val vm = LeadsViewModel(FakeBackend(asRealtor = true, seed = false).leads)
        assertTrue(vm.state.value.visible.isEmpty())
        assertNull(vm.state.value.error)
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class LeadDetailViewModelTest {
    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @After fun tearDown() = Dispatchers.resetMain()

    private fun vm(backend: FakeBackend, id: Long) = LeadDetailViewModel(id, backend.leads, backend.agenda)

    @Test fun marksReadOnOpen() {
        val backend = FakeBackend(asRealtor = true)
        assertFalse(backend.inquiryRows.first { it.id == 101L }.read)
        val vm = vm(backend, 101)
        assertTrue(backend.inquiryRows.first { it.id == 101L }.read)
        assertEquals("Carlos Lima", vm.state.value.lead?.name)
        assertTrue(vm.state.value.lead?.read == true)
    }

    @Test fun changesStageAndPriority() {
        val backend = FakeBackend(asRealtor = true)
        val vm = vm(backend, 101)
        vm.setStage(InquiryStage.QUALIFIED)
        assertEquals(InquiryStage.QUALIFIED, backend.inquiryRows.first { it.id == 101L }.stage)
        assertEquals(InquiryStage.QUALIFIED, vm.state.value.lead?.stage)
        assertEquals("Etapa alterada para Qualificado", vm.state.value.message)
        vm.messageShown()
        vm.setPriority(Priority.LOW)
        assertEquals(Priority.LOW, backend.inquiryRows.first { it.id == 101L }.priority)
        assertEquals(Priority.LOW, vm.state.value.lead?.priority)
    }

    @Test fun addsNoteNewestFirst() {
        val backend = FakeBackend(asRealtor = true)
        val vm = vm(backend, 103)
        assertEquals(1, vm.state.value.notes.size)
        vm.setNoteDraft("  Ligar amanhã às 9h  ")
        vm.addNote()
        assertTrue(backend.noteRows.any { it.inquiryId == 103L && it.body == "Ligar amanhã às 9h" })
        val notes = vm.state.value.notes
        assertEquals(2, notes.size)
        assertEquals("Ligar amanhã às 9h", notes.first().body)
        assertEquals("", vm.state.value.noteDraft)
        assertEquals("Nota adicionada", vm.state.value.message)
    }

    @Test fun blankNoteIsIgnored() {
        val backend = FakeBackend(asRealtor = true)
        val vm = vm(backend, 101)
        vm.setNoteDraft("   ")
        vm.addNote()
        assertTrue(backend.noteRows.none { it.inquiryId == 101L })
    }

    @Test fun phoneFromSeededShowings() {
        val backend = FakeBackend(asRealtor = true)
        assertEquals("(11) 91234-5678", vm(backend, 104).state.value.phone) // Juliana Prado
        assertEquals("(11) 91234-5678", vm(backend, 103).state.value.phone) // Rafael Torres
        assertNull(vm(backend, 101).state.value.phone) // Carlos Lima: no showing
    }

    @Test fun missingLeadShowsError() {
        val vm = vm(FakeBackend(asRealtor = true), 999)
        assertNull(vm.state.value.lead)
        assertEquals("Lead não encontrado", vm.state.value.error)
    }
}
