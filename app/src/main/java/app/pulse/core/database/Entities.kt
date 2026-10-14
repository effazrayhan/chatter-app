package app.pulse.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import app.pulse.core.model.MessageState
import java.util.UUID

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: UUID,
    val username: String?,
    val displayName: String,
    val avatarPath: String?,
    val bio: String,
    val lastSeen: Long,
)

@Entity(tableName = "chats")
data class ChatEntity(
    @PrimaryKey val id: UUID,
    val type: String,
    val title: String,
    val lastMessageAt: Long,
    val pinned: Boolean,
    val unreadCount: Int,
    val draft: String,
)

@Entity(tableName = "messages", primaryKeys = ["id"])
data class MessageEntity(
    val id: UUID,
    val chatId: UUID,
    val senderId: UUID,
    val body: String,
    val seq: Long?,
    val localCreatedAt: Long,
    val createdAt: Long,
    val localState: MessageState,
    val retryCount: Int,
    val replyToId: UUID?,
    val editedAt: Long?,
    val deletedAt: Long?,
)

@Entity(tableName = "drafts")
data class DraftEntity(
    @PrimaryKey val chatId: UUID,
    val text: String,
    val updatedAt: Long,
)

@Entity(tableName = "sync_state")
data class SyncStateEntity(
    @PrimaryKey val chatId: UUID,
    val lastSeq: Long,
)

@Entity(tableName = "outbox")
data class OutboxEntity(
    @PrimaryKey val messageId: UUID,
    val chatId: UUID,
    val createdLocal: Long,
    val retryCount: Int,
)
