package app.pulse.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.pulse.core.AppContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AuthUiState(
    val email: String = "",
    val password: String = "",
    val displayName: String = "",
    val loading: Boolean = false,
    val error: String? = null,
    val signedIn: Boolean = false,
)

class AuthViewModel : ViewModel() {
    private val mutableState = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = mutableState.asStateFlow()

    fun emailChanged(value: String) {
        mutableState.value = mutableState.value.copy(email = value, error = null)
    }

    fun passwordChanged(value: String) {
        mutableState.value = mutableState.value.copy(password = value, error = null)
    }

    fun displayNameChanged(value: String) {
        mutableState.value = mutableState.value.copy(displayName = value, error = null)
    }

    fun submit(register: Boolean) {
        val current = mutableState.value
        if (!current.email.contains('@') || current.password.length < 6) {
            mutableState.value = current.copy(error = "Enter a valid email and a password of at least 6 characters")
            return
        }
        viewModelScope.launch {
            mutableState.value = current.copy(loading = true, error = null)
            val result =
                if (register) {
                    AppContainer.authRepository.signUp(
                        current.email,
                        current.password,
                        current.displayName,
                    )
                } else {
                    AppContainer.authRepository.signIn(current.email, current.password)
                }
            mutableState.value =
                result.fold({
                    current.copy(loading = false, signedIn = true)
                }, { current.copy(loading = false, error = it.message ?: "Authentication failed") })
        }
    }
}
