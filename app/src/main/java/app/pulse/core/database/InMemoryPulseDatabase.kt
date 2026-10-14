package app.pulse.core.database

import app.pulse.core.model.Chat
import app.pulse.core.model.Message
import app.pulse.core.model.MessageState
import app.pulse.core.model.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

class InMemoryPulseDatabase {
    private val mutex = Mutex()
    private val chats = MutableStateFlow<Map<UUID, ChatEntity>>(emptyMap())
    private val messages = MutableStateFlow<Map<UUID, MessageEntity>>(emptyMap())
    private val users = MutableStateFlow<Map<UUID, UserEntity>>(emptyMap())

    fun observeChats(): Flow<List<Chat>> =
        chats.map { values ->
            values.values.sortedWith(
                compareByDescending<ChatEntity> { it.pinned }.thenByDescending { it.lastMessageAt },
            ).map(ChatEntity::toModel)
        }

    fun observeMessages(chatId: UUID): Flow<List<Message>> =
        messages.map { values ->
            values.values.filter {
                it.chatId == chatId
            }.sortedWith(compareBy<MessageEntity> { it.seq ?: Long.MAX_VALUE }.thenBy { it.localCreatedAt }).map(MessageEntity::toModel)
        }

    fun observeUsers(): Flow<List<User>> = users.map { it.values.map(UserEntity::toModel) }

    suspend fun saveChat(chat: Chat) = mutex.withLock { chats.value = chats.value + (chat.id to chat.toEntity()) }

    suspend fun saveMessage(
        message: Message,
        retryCount: Int = 0,
    ) = mutex.withLock {
        messages.value = messages.value + (message.id to message.toEntity(retryCount))
    }

    suspend fun saveUser(user: User) = mutex.withLock { users.value = users.value + (user.id to user.toEntity()) }

    suspend fun pendingMessages(): List<Message> =
        messages.value.values.filter {
            it.localState == MessageState.PENDING
        }.sortedBy { it.localCreatedAt }.map(MessageEntity::toModel)

    suspend fun updateMessage(
        message: Message,
        retryCount: Int = 0,
    ) = saveMessage(message, retryCount)

    suspend fun clear() =
        mutex.withLock {
            chats.value = emptyMap()
            messages.value = emptyMap()
            users.value = emptyMap()
        }
}

private fun Chat.toEntity() = ChatEntity(id, type.name, title, lastMessageAt, pinned, unreadCount, draft)

private fun ChatEntity.toModel() = Chat(UUID.fromString(id.toString()), enumValueOf(type), title, lastMessageAt, pinned, unreadCount, draft)

private fun Message.toEntity(retryCount: Int) =
    MessageEntity(id, chatId, senderId, body, seq, localCreatedAt, createdAt, state, retryCount, replyToId, editedAt, deletedAt)

private fun MessageEntity.toModel() =
    Message(id, chatId, senderId, body, seq, localCreatedAt, createdAt, localState, replyToId, editedAt, deletedAt)

private fun User.toEntity() = UserEntity(id, username, displayName, avatarPath, bio, lastSeen)

private fun UserEntity.toModel() = User(id, username, displayName, avatarPath, bio, lastSeen)
