package app.pulse.core.model

import java.util.UUID

data class User(
    val id: UUID,
    val username: String?,
    val displayName: String,
    val avatarPath: String? = null,
    val bio: String = "",
    val lastSeen: Long = 0L,
)

enum class ChatType { DIRECT, GROUP, CHANNEL, SAVED }

data class Chat(
    val id: UUID,
    val type: ChatType,
    val title: String,
    val lastMessageAt: Long,
    val pinned: Boolean = false,
    val unreadCount: Int = 0,
    val draft: String = "",
)

enum class MessageState { PENDING, SENT, DELIVERED, READ, FAILED }

data class Message(
    val id: UUID,
    val chatId: UUID,
    val senderId: UUID,
    val body: String,
    val seq: Long? = null,
    val localCreatedAt: Long = System.currentTimeMillis(),
    val createdAt: Long = localCreatedAt,
    val state: MessageState = MessageState.PENDING,
    val replyToId: UUID? = null,
    val editedAt: Long? = null,
    val deletedAt: Long? = null,
)

data class Draft(val chatId: UUID, val text: String, val updatedAt: Long)

data class SyncState(val chatId: UUID, val lastSeq: Long)

data class OutboxItem(val message: Message, val retryCount: Int = 0)
