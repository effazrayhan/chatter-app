package app.pulse.feature.profile

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ProfileUiState(
    val displayName: String = "",
    val username: String = "",
    val bio: String = "",
    val error: String? = null,
    val saved: Boolean = false,
)

class ProfileViewModel : ViewModel() {
    private val mutableState = MutableStateFlow(ProfileUiState())
    val state: StateFlow<ProfileUiState> = mutableState.asStateFlow()

    fun displayNameChanged(value: String) {
        mutableState.value = mutableState.value.copy(displayName = value, saved = false)
    }

    fun usernameChanged(value: String) {
        val error =
            if (value.matches(Regex("[A-Za-z0-9_]{3,32}"))) {
                null
            } else {
                "Use 3–32 letters, numbers, or underscores"
            }
        mutableState.value = mutableState.value.copy(username = value, saved = false, error = error)
    }

    fun bioChanged(value: String) {
        mutableState.value = mutableState.value.copy(bio = value, saved = false)
    }

    fun save() {
        if (mutableState.value.error == null) mutableState.value = mutableState.value.copy(saved = true)
    }
}
