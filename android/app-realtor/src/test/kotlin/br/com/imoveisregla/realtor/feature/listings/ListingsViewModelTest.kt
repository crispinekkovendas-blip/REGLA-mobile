package br.com.imoveisregla.realtor.feature.listings

import br.com.imoveisregla.core.data.fake.FakeBackend
import br.com.imoveisregla.core.model.ListingStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ListingsViewModelTest {
    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @After fun tearDown() = Dispatchers.resetMain()

    @Test fun statusCountsFromSeed() {
        val vm = ListingsViewModel(FakeBackend(asRealtor = true).listings)
        val s = vm.state.value
        assertFalse(s.loading)
        assertEquals(8, s.all.size)
        assertEquals(7, s.counts[ListingStatus.LIVE])
        assertEquals(1, s.counts[ListingStatus.DRAFT])
        assertEquals(0, s.counts[ListingStatus.SOLD])
        assertEquals(0, s.counts[ListingStatus.WITHDRAWN])
    }

    @Test fun statusFilterAndSearch() {
        val vm = ListingsViewModel(FakeBackend(asRealtor = true).listings)
        vm.setStatusFilter(ListingStatus.DRAFT)
        assertEquals(listOf(8L), vm.state.value.visible.map { it.id })
        vm.setStatusFilter(null)
        vm.setQuery("moema")
        assertEquals(listOf(7L), vm.state.value.visible.map { it.id })
        vm.setQuery("RG-0004")
        assertEquals(listOf(4L), vm.state.value.visible.map { it.id })
    }

    @Test fun setStatusUpdatesCounts() {
        val backend = FakeBackend(asRealtor = true)
        val vm = ListingsViewModel(backend.listings)
        vm.setStatus(4, ListingStatus.SOLD)
        assertEquals(ListingStatus.SOLD, backend.listingRows.first { it.id == 4L }.status)
        assertEquals(6, vm.state.value.counts[ListingStatus.LIVE])
        assertEquals(1, vm.state.value.counts[ListingStatus.SOLD])
        assertEquals("Status alterado para Vendido", vm.state.value.message)
    }

    @Test fun detailLoadsAndChangesStatus() {
        val backend = FakeBackend(asRealtor = true)
        val vm = ListingDetailViewModel(8, backend.listings)
        assertEquals(ListingStatus.DRAFT, vm.state.value.listing?.status)
        vm.setStatus(ListingStatus.LIVE)
        assertEquals(ListingStatus.LIVE, backend.listingRows.first { it.id == 8L }.status)
        assertEquals(ListingStatus.LIVE, vm.state.value.listing?.status)
        assertFalse(vm.state.value.saving)
    }

    @Test fun helpers() {
        val l = FakeBackend(asRealtor = true).listingRows
        val rental = l.first { it.id == 1L }
        val sale = l.first { it.id == 4L }
        assertEquals("R$ 4.800/mês", listingPrice(rental))
        assertEquals("R$ 3.950.000", listingPrice(sale))
        assertEquals("https://imoveisregla.com.br/imovel/demo-1", publicListingUrl(rental.slug))
        assertEquals("${rental.title}\nhttps://imoveisregla.com.br/imovel/demo-1", shareText(rental))
        assertTrue(needsConfirmation(ListingStatus.SOLD))
        assertTrue(needsConfirmation(ListingStatus.WITHDRAWN))
        assertFalse(needsConfirmation(ListingStatus.LIVE))
        assertEquals("2 quartos · 2 banheiros · 72 m²", specsLine(rental))
    }
}
