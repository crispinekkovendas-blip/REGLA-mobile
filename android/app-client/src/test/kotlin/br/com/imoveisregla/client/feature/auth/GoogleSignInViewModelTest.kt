package br.com.imoveisregla.client.feature.auth

import br.com.imoveisregla.core.data.SessionState
import br.com.imoveisregla.core.data.fake.FakeBackend
import br.com.imoveisregla.core.data.google.GoogleIdToken
import br.com.imoveisregla.core.data.google.GoogleSignInException
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
class GoogleSignInViewModelTest {

    @Before fun setUp() { Dispatchers.setMain(UnconfinedTestDispatcher()) }
    @After fun tearDown() { Dispatchers.resetMain() }

    @Test fun `token from the picker signs the user in`() {
        val db = FakeBackend(asRealtor = false, signedIn = false)
        val vm = GoogleSignInViewModel(db.auth)
        vm.signIn { GoogleIdToken(idToken = "id-token", rawNonce = "nonce") }
        assertTrue(vm.state.value.signedIn)
        assertFalse(vm.state.value.loading)
        assertTrue(db.auth.session.value is SessionState.SignedIn)
    }

    @Test fun `closing the picker is not an error`() {
        val db = FakeBackend(asRealtor = false, signedIn = false)
        val vm = GoogleSignInViewModel(db.auth)
        vm.signIn { null }
        assertFalse(vm.state.value.signedIn)
        assertNull(vm.state.value.error)
        assertEquals(SessionState.SignedOut, db.auth.session.value)
    }

    @Test fun `setup or picker errors are shown`() {
        val db = FakeBackend(asRealtor = false, signedIn = false)
        val vm = GoogleSignInViewModel(db.auth)
        vm.signIn { throw GoogleSignInException("Login com Google ainda não está disponível. Use e-mail e senha por enquanto.") }
        assertEquals("Login com Google ainda não está disponível. Use e-mail e senha por enquanto.", vm.state.value.error)
        assertFalse(vm.state.value.loading)
        vm.clearError()
        assertNull(vm.state.value.error)
    }
}
