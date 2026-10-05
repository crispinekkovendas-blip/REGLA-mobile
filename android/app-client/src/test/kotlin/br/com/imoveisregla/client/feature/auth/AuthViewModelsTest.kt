package br.com.imoveisregla.client.feature.auth

import br.com.imoveisregla.core.data.SessionState
import br.com.imoveisregla.core.data.fake.FakeBackend
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
class AuthViewModelsTest {

    @Before fun setUp() { Dispatchers.setMain(UnconfinedTestDispatcher()) }

    @After fun tearDown() { Dispatchers.resetMain() }

    private fun signedOutBackend() = FakeBackend(asRealtor = false, signedIn = false)

    @Test fun login_invalidEmail_showsFieldError_andDoesNotSignIn() {
        val backend = signedOutBackend()
        val vm = LoginViewModel(backend.auth)
        vm.onEmailChange("nao-e-email")
        vm.onPasswordChange("segredo123")
        vm.submit()

        val s = vm.state.value
        assertEquals("Informe um e-mail válido", s.emailError)
        assertFalse(s.signedIn)
        assertFalse(s.loading)
        assertTrue(backend.auth.session.value is SessionState.SignedOut)
    }

    @Test fun login_emptyPassword_showsPasswordError() {
        val vm = LoginViewModel(signedOutBackend().auth)
        vm.onEmailChange("ana@exemplo.com")
        vm.submit()
        assertNull(vm.state.value.emailError)
        assertEquals("Informe sua senha", vm.state.value.passwordError)
    }

    @Test fun login_success_signsIn() {
        val backend = signedOutBackend()
        val vm = LoginViewModel(backend.auth)
        vm.onEmailChange("  ana@exemplo.com ")
        vm.onPasswordChange("segredo123")
        vm.submit()

        val s = vm.state.value
        assertTrue(s.signedIn)
        assertFalse(s.loading)
        assertNull(s.error)
        val session = backend.auth.session.value
        assertTrue(session is SessionState.SignedIn)
        assertEquals("ana@exemplo.com", (session as SessionState.SignedIn).session.email)
    }

    @Test fun login_backendError_isShown() {
        val backend = signedOutBackend()
        val vm = LoginViewModel(backend.auth)
        vm.onEmailChange("ana@exemplo.com")
        vm.onPasswordChange("123") // FakeBackend rejects < 6 chars
        vm.submit()

        val s = vm.state.value
        assertFalse(s.signedIn)
        assertEquals("Senha incorreta", s.error)
        // Editing a field clears the banner
        vm.onPasswordChange("1234")
        assertNull(vm.state.value.error)
    }

    @Test fun login_togglePasswordVisibility() {
        val vm = LoginViewModel(signedOutBackend().auth)
        assertFalse(vm.state.value.passwordVisible)
        vm.togglePasswordVisible()
        assertTrue(vm.state.value.passwordVisible)
    }

    @Test fun signup_passwordMismatch_showsConfirmError() {
        val backend = signedOutBackend()
        val vm = SignupViewModel(backend.auth)
        vm.onEmailChange("ana@exemplo.com")
        vm.onPasswordChange("segredo123")
        vm.onConfirmChange("segredo321")
        vm.submit()

        val s = vm.state.value
        assertEquals("As senhas não coincidem", s.confirmError)
        assertNull(s.passwordError)
        assertFalse(s.signedUp)
        assertTrue(backend.auth.session.value is SessionState.SignedOut)
    }

    @Test fun signup_shortPassword_showsPasswordError() {
        val vm = SignupViewModel(signedOutBackend().auth)
        vm.onEmailChange("ana@exemplo.com")
        vm.onPasswordChange("123")
        vm.onConfirmChange("123")
        vm.submit()
        assertNotNull(vm.state.value.passwordError)
        assertFalse(vm.state.value.signedUp)
    }

    @Test fun signup_success_createsSession() {
        val backend = signedOutBackend()
        val vm = SignupViewModel(backend.auth)
        vm.onEmailChange("novo@exemplo.com")
        vm.onPasswordChange("segredo123")
        vm.onConfirmChange("segredo123")
        vm.submit()

        assertTrue(vm.state.value.signedUp)
        assertFalse(vm.state.value.needsConfirmation)
        assertTrue(backend.auth.session.value is SessionState.SignedIn)
    }

    @Test fun errorMessages_areTranslated() {
        assertEquals(
            "E-mail ou senha incorretos",
            authErrorMessage(IllegalStateException("Invalid login credentials"), "x"),
        )
        assertEquals("fallback", authErrorMessage(IllegalStateException(), "fallback"))
    }
}
