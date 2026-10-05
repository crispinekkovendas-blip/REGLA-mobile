package br.com.imoveisregla.client.feature.search

import br.com.imoveisregla.core.data.fake.FakeBackend
import br.com.imoveisregla.core.model.ListingType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {

    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @After fun tearDown() = Dispatchers.resetMain()

    private fun vm(signedIn: Boolean = true): Pair<SearchViewModel, FakeBackend> {
        val backend = FakeBackend(asRealtor = false, signedIn = signedIn)
        return SearchViewModel(backend) to backend
    }

    private fun SearchViewModel.ids(): Set<Long> = state.value.listings.map { it.id }.toSet()

    @Test fun `initial load shows only live listings`() = runTest {
        val (vm, _) = vm()
        val s = vm.state.value
        assertFalse(s.loading)
        assertNull(s.error)
        assertEquals((1L..7L).toSet(), vm.ids())
        assertEquals(listOf("São Paulo"), s.cities)
    }

    @Test fun `type filter`() = runTest {
        val (vm, _) = vm()
        vm.setType(ListingType.HOUSE)
        assertEquals(setOf(3L, 7L), vm.ids())
        vm.setType(null)
        assertEquals(7, vm.state.value.listings.size)
    }

    @Test fun `min beds filter`() = runTest {
        val (vm, _) = vm()
        vm.setMinBeds(3)
        assertEquals(setOf(3L, 4L, 7L), vm.ids())
    }

    @Test fun `max price filter`() = runTest {
        val (vm, _) = vm()
        vm.setMaxPrice(5_000)
        assertEquals(setOf(1L, 2L, 5L), vm.ids())
    }

    @Test fun `filters combine`() = runTest {
        val (vm, _) = vm()
        vm.setType(ListingType.APARTMENT)
        vm.setMaxPrice(5_000)
        vm.setMinBeds(2)
        assertEquals(setOf(1L, 5L), vm.ids())
        vm.clearFilters()
        assertEquals(7, vm.state.value.listings.size)
    }

    @Test fun `query is debounced`() = runTest {
        val (vm, _) = vm()
        vm.setQuery("pinheiros")
        assertEquals("pinheiros", vm.state.value.query)
        advanceTimeBy(SEARCH_DEBOUNCE_MS - 50)
        assertEquals(7, vm.state.value.listings.size)
        advanceTimeBy(100)
        assertEquals(setOf(1L), vm.ids())
        assertEquals("Apartamento com varanda em Pinheiros", vm.state.value.listings.single().title)
    }

    @Test fun `query with no match yields empty results`() = runTest {
        val (vm, _) = vm()
        vm.setQuery("Copacabana")
        advanceUntilIdle()
        assertTrue(vm.state.value.listings.isEmpty())
        assertFalse(vm.state.value.loading)
    }

    @Test fun `favorite toggle persists to backend`() = runTest {
        val (vm, backend) = vm()
        vm.toggleFavorite(1)
        assertTrue(1L in vm.state.value.favoriteIds)
        assertTrue(("client-1" to 1L) in backend.favoriteRows)
        vm.toggleFavorite(1)
        assertFalse(1L in vm.state.value.favoriteIds)
        assertTrue(backend.favoriteRows.isEmpty())
    }

    @Test fun `favorite toggle signed out shows login message`() = runTest {
        val (vm, backend) = vm(signedIn = false)
        assertEquals(7, vm.state.value.listings.size)
        vm.toggleFavorite(1)
        assertEquals(FAVORITE_LOGIN_MESSAGE, vm.state.value.message)
        assertTrue(vm.state.value.favoriteIds.isEmpty())
        assertTrue(backend.favoriteRows.isEmpty())
        vm.messageShown()
        assertNull(vm.state.value.message)
    }

    @Test fun `favorites reload after sign in`() = runTest {
        val (vm, backend) = vm(signedIn = false)
        backend.favoriteRows += "client-1" to 3L
        backend.auth.signIn("cliente@exemplo.com", "123456")
        assertEquals(setOf(3L), vm.state.value.favoriteIds)
    }

    @Test fun `labels`() {
        assertEquals("3 imóveis encontrados", resultsLabel(3))
        assertEquals("1 imóvel encontrado", resultsLabel(1))
        assertEquals("Sem limite", priceOptionLabel(null))
        assertTrue(priceOptionLabel(3_000).startsWith("Até R$"))
        assertTrue(priceOptionLabel(3_000).endsWith("3.000"))
    }
}
