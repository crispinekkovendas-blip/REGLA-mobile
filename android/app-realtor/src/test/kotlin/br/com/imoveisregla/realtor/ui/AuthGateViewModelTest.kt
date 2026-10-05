package br.com.imoveisregla.realtor.ui

import br.com.imoveisregla.core.data.fake.FakeBackend
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthGateViewModelTest {

    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @After fun tearDown() = Dispatchers.resetMain()

    @Test
    fun realtorSessionIsReady() = runTest {
        val vm = AuthGateViewModel(FakeBackend(asRealtor = true).auth)
        advanceUntilIdle()
        val s = vm.state.value
        assertTrue("expected Ready, got $s", s is GateState.Ready)
        assertEquals("realtor-1", (s as GateState.Ready).session.userId)
    }

    @Test
    fun nonRealtorIsRestricted() = runTest {
        val vm = AuthGateViewModel(FakeBackend(asRealtor = false).auth)
        advanceUntilIdle()
        assertEquals(GateState.Restricted("cliente@exemplo.com"), vm.state.value)
    }

    @Test
    fun signedOutRequiresLogin() = runTest {
        val vm = AuthGateViewModel(FakeBackend(asRealtor = true, signedIn = false).auth)
        advanceUntilIdle()
        assertEquals(GateState.LoginRequired, vm.state.value)
    }

    @Test
    fun signingInMovesToReady() = runTest {
        val fake = FakeBackend(asRealtor = true, signedIn = false)
        val vm = AuthGateViewModel(fake.auth)
        advanceUntilIdle()
        fake.auth.signIn("corretor@imoveisregla.com.br", "segredo123")
        advanceUntilIdle()
        assertTrue(vm.state.value is GateState.Ready)
    }

    @Test
    fun restrictedUserCanSignOut() = runTest {
        val vm = AuthGateViewModel(FakeBackend(asRealtor = false).auth)
        advanceUntilIdle()
        vm.signOut()
        advanceUntilIdle()
        assertEquals(GateState.LoginRequired, vm.state.value)
    }
}
