package app.pulse.feature.chatlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.pulse.core.AppContainer
import app.pulse.core.model.Chat
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class ChatListViewModel : ViewModel() {
    val chats: StateFlow<List<Chat>> =
        AppContainer.chatRepository.observeChats().stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            emptyList(),
        )
}
