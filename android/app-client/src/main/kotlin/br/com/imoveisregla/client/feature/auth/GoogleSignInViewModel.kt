package br.com.imoveisregla.client.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.imoveisregla.core.data.AuthRepository
import br.com.imoveisregla.core.data.google.GoogleIdToken
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class GoogleSignInUiState(
    val loading: Boolean = false,
    val error: String? = null,
    val signedIn: Boolean = false,
)

/**
 * "Continuar com o Google" for the login and signup screens. The screen passes [requestToken]
 * (Credential Manager needs an Activity context), so this stays unit-testable.
 */
class GoogleSignInViewModel(private val auth: AuthRepository) : ViewModel() {
    private val _state = MutableStateFlow(GoogleSignInUiState())
    val state: StateFlow<GoogleSignInUiState> = _state.asStateFlow()

    fun signIn(requestToken: suspend () -> GoogleIdToken?) {
        if (_state.value.loading) return
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            try {
                val token = requestToken()
                if (token == null) {
                    _state.update { it.copy(loading = false) } // user closed the picker
                    return@launch
                }
                auth.signInWithGoogle(token.idToken, token.rawNonce)
                _state.update { it.copy(loading = false, signedIn = true) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update {
                    it.copy(loading = false, error = authErrorMessage(e, "Não foi possível entrar com o Google. Tente novamente."))
                }
            }
        }
    }

    fun clearError() = _state.update { it.copy(error = null) }
}
