package br.com.imoveisregla.realtor.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.imoveisregla.core.data.AuthRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val passwordVisible: Boolean = false,
    val loading: Boolean = false,
    val error: String? = null,
) {
    val canSubmit: Boolean get() = email.isNotBlank() && password.isNotEmpty() && !loading
}

class LoginViewModel(private val auth: AuthRepository) : ViewModel() {
    private val _state = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    fun onEmail(v: String) = _state.update { it.copy(email = v.trim(), error = null) }
    fun onPassword(v: String) = _state.update { it.copy(password = v, error = null) }
    fun togglePassword() = _state.update { it.copy(passwordVisible = !it.passwordVisible) }

    fun submit(onSuccess: () -> Unit) {
        val s = _state.value
        if (s.loading) return
        val email = s.email.trim()
        when {
            email.isEmpty() || !email.contains("@") -> { _state.update { it.copy(error = "Informe um e-mail válido") }; return }
            s.password.isEmpty() -> { _state.update { it.copy(error = "Informe sua senha") }; return }
        }
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            try {
                auth.signIn(email, s.password)
                _state.update { it.copy(loading = false, password = "") }
                onSuccess()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(loading = false, error = friendlyAuthError(e.message)) }
            }
        }
    }
}

internal fun friendlyAuthError(raw: String?): String {
    val m = raw.orEmpty()
    return when {
        m.contains("invalid login", true) || m.contains("invalid_credentials", true) -> "E-mail ou senha incorretos"
        m.contains("email not confirmed", true) -> "Confirme seu e-mail antes de entrar"
        m.contains("network", true) || m.contains("unable to resolve", true) || m.contains("timeout", true) ->
            "Sem conexão. Verifique sua internet e tente novamente"
        m.isBlank() -> "Não foi possível entrar. Tente novamente"
        else -> m
    }
}
