package app.pulse.feature.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.pulse.core.AppContainer
import app.pulse.core.model.Message
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class ChatViewModel(private val chatId: UUID) : ViewModel() {
    val messages: StateFlow<List<Message>> =
        AppContainer.messageRepository.observeMessages(
            chatId,
        ).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun send(text: String) {
        val body = text.trim().takeIf { it.isNotEmpty() } ?: return
        viewModelScope.launch {
            AppContainer.messageRepository.enqueue(Message(UUID.randomUUID(), chatId, AppContainer.currentUserId, body))
            AppContainer.messageRepository.sendPending()
        }
    }
}
