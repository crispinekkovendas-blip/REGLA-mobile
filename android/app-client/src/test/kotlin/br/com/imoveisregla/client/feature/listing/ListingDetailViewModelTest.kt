package br.com.imoveisregla.client.feature.listing

import br.com.imoveisregla.client.feature.search.displayPrice
import br.com.imoveisregla.core.data.fake.FakeBackend
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ListingDetailViewModelTest {

    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @After fun tearDown() = Dispatchers.resetMain()

    @Test fun `loads listing with photos`() = runTest {
        val vm = ListingDetailViewModel(FakeBackend(asRealtor = false), 1)
        val s = vm.state.value as ListingDetailUiState.Loaded
        assertEquals("Apartamento com varanda em Pinheiros", s.listing.title)
        assertEquals(3, s.photoUrls.size)
        assertFalse(s.isFavorite)
        assertTrue(s.listing.displayPrice().endsWith("/mês"))
    }

    @Test fun `sale listing has no monthly suffix`() = runTest {
        val vm = ListingDetailViewModel(FakeBackend(asRealtor = false), 4)
        val s = vm.state.value as ListingDetailUiState.Loaded
        assertFalse(s.listing.displayPrice().endsWith("/mês"))
    }

    @Test fun `unknown id is not found`() = runTest {
        val vm = ListingDetailViewModel(FakeBackend(asRealtor = false), 999)
        assertEquals(ListingDetailUiState.NotFound, vm.state.value)
    }

    @Test fun `reflects and toggles favorite`() = runTest {
        val backend = FakeBackend(asRealtor = false)
        backend.favoriteRows += "client-1" to 2L
        val vm = ListingDetailViewModel(backend, 2)
        assertTrue((vm.state.value as ListingDetailUiState.Loaded).isFavorite)
        vm.toggleFavorite()
        assertFalse((vm.state.value as ListingDetailUiState.Loaded).isFavorite)
        assertTrue(backend.favoriteRows.isEmpty())
    }

    @Test fun `signed out loads without favorite`() = runTest {
        val vm = ListingDetailViewModel(FakeBackend(asRealtor = false, signedIn = false), 1)
        assertFalse(vm.isSignedIn)
        assertFalse((vm.state.value as ListingDetailUiState.Loaded).isFavorite)
    }
}
