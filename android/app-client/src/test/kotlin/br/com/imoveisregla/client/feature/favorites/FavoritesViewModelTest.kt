package br.com.imoveisregla.client.feature.favorites

import br.com.imoveisregla.core.data.fake.FakeBackend
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FavoritesViewModelTest {

    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @After fun tearDown() = Dispatchers.resetMain()

    @Test fun `signed out state`() = runTest {
        val vm = FavoritesViewModel(FakeBackend(asRealtor = false, signedIn = false))
        assertEquals(FavoritesUiState.SignedOut, vm.state.value)
        vm.load()
        assertEquals(FavoritesUiState.SignedOut, vm.state.value)
    }

    @Test fun `signed in with no favorites is empty`() = runTest {
        val vm = FavoritesViewModel(FakeBackend(asRealtor = false))
        assertEquals(FavoritesUiState.Loaded(emptyList()), vm.state.value)
    }

    @Test fun `lists favorites and removes them`() = runTest {
        val backend = FakeBackend(asRealtor = false)
        backend.favoriteRows += "client-1" to 1L
        backend.favoriteRows += "client-1" to 4L
        val vm = FavoritesViewModel(backend)
        val loaded = vm.state.value as FavoritesUiState.Loaded
        assertEquals(setOf(1L, 4L), loaded.listings.map { it.id }.toSet())

        vm.remove(1)
        assertEquals(listOf(4L), (vm.state.value as FavoritesUiState.Loaded).listings.map { it.id })
        assertEquals(setOf("client-1" to 4L), backend.favoriteRows)
    }

    @Test fun `signing in loads favorites, signing out clears`() = runTest {
        val backend = FakeBackend(asRealtor = false, signedIn = false)
        backend.favoriteRows += "client-1" to 3L
        val vm = FavoritesViewModel(backend)
        assertEquals(FavoritesUiState.SignedOut, vm.state.value)
        backend.auth.signIn("cliente@exemplo.com", "123456")
        assertEquals(listOf(3L), (vm.state.value as FavoritesUiState.Loaded).listings.map { it.id })
        backend.auth.signOut()
        assertTrue(vm.state.value is FavoritesUiState.SignedOut)
    }
}
