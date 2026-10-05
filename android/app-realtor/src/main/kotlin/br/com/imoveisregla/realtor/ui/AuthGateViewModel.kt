package br.com.imoveisregla.realtor.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.imoveisregla.core.data.AuthRepository
import br.com.imoveisregla.core.data.SessionState
import br.com.imoveisregla.core.model.UserSession
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/** What the realtor app shell should show. */
sealed interface GateState {
    data object Loading : GateState
    data object LoginRequired : GateState
    /** Signed in, but the account is not a REGLA realtor. */
    data class Restricted(val email: String) : GateState
    /** Could not verify the realtor role (network etc.). */
    data class Failed(val message: String) : GateState
    data class Ready(val session: UserSession) : GateState
}

class AuthGateViewModel(private val auth: AuthRepository) : ViewModel() {
    private val _state = MutableStateFlow<GateState>(GateState.Loading)
    val state: StateFlow<GateState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            auth.session.collectLatest { evaluate(it) }
        }
    }

    private suspend fun evaluate(s: SessionState) {
        _state.value = when (s) {
            SessionState.Loading -> GateState.Loading
            SessionState.SignedOut -> GateState.LoginRequired
            is SessionState.SignedIn -> {
                _state.value = GateState.Loading
                try {
                    if (auth.isRealtor()) GateState.Ready(s.session) else GateState.Restricted(s.session.email)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    GateState.Failed(e.message ?: "Não foi possível verificar seu acesso")
                }
            }
        }
    }

    fun retry() {
        viewModelScope.launch { evaluate(auth.session.value) }
    }

    fun signOut() {
        viewModelScope.launch {
            try {
                auth.signOut()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // Session flow stays as-is; the user can try again.
            }
        }
    }
}
