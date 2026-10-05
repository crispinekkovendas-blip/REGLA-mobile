package br.com.imoveisregla.client.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.imoveisregla.core.data.AuthRepository
import br.com.imoveisregla.core.data.SessionState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SignupUiState(
    val email: String = "",
    val password: String = "",
    val confirm: String = "",
    val passwordVisible: Boolean = false,
    val emailError: String? = null,
    val passwordError: String? = null,
    val confirmError: String? = null,
    val error: String? = null,
    val loading: Boolean = false,
    /** Account created and a session is active. */
    val signedUp: Boolean = false,
    /** Account created but the backend requires e-mail confirmation before login. */
    val needsConfirmation: Boolean = false,
)

class SignupViewModel(private val auth: AuthRepository) : ViewModel() {
    private val _state = MutableStateFlow(SignupUiState())
    val state: StateFlow<SignupUiState> = _state.asStateFlow()

    fun onEmailChange(value: String) = _state.update { it.copy(email = value, emailError = null, error = null) }

    fun onPasswordChange(value: String) = _state.update { it.copy(password = value, passwordError = null, error = null) }

    fun onConfirmChange(value: String) = _state.update { it.copy(confirm = value, confirmError = null, error = null) }

    fun togglePasswordVisible() = _state.update { it.copy(passwordVisible = !it.passwordVisible) }

    fun submit() {
        val s = _state.value
        if (s.loading) return
        val emailError = if (!isValidEmail(s.email)) "Informe um e-mail válido" else null
        val passwordError = when {
            s.password.length < MIN_PASSWORD_LENGTH -> "A senha precisa de pelo menos $MIN_PASSWORD_LENGTH caracteres"
            else -> null
        }
        val confirmError = when {
            s.confirm.isEmpty() -> "Confirme sua senha"
            s.confirm != s.password -> "As senhas não coincidem"
            else -> null
        }
        if (emailError != null || passwordError != null || confirmError != null) {
            _state.update {
                it.copy(emailError = emailError, passwordError = passwordError, confirmError = confirmError, error = null)
            }
            return
        }
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            try {
                auth.signUp(s.email.trim(), s.password)
                val active = auth.session.value is SessionState.SignedIn
                _state.update { it.copy(loading = false, signedUp = active, needsConfirmation = !active) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update {
                    it.copy(loading = false, error = authErrorMessage(e, "Não foi possível criar sua conta. Tente novamente."))
                }
            }
        }
    }
}
