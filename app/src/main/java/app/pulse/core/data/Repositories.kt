package app.pulse.core.data

import app.pulse.core.database.InMemoryPulseDatabase
import app.pulse.core.model.Chat
import app.pulse.core.model.Message
import app.pulse.core.model.MessageState
import app.pulse.core.model.User
import kotlinx.coroutines.flow.Flow
import java.util.UUID

interface AuthRepository {
    val session: Flow<UUID?>

    suspend fun signIn(
        email: String,
        password: String,
    ): Result<UUID>

    suspend fun signUp(
        email: String,
        password: String,
        displayName: String,
    ): Result<UUID>

    suspend fun signOut()
}

interface ChatRepository {
    fun observeChats(): Flow<List<Chat>>

    fun observeUsers(): Flow<List<User>>

    suspend fun createDirectChat(other: UUID): Result<UUID>
}

interface MessageRepository {
    fun observeMessages(chatId: UUID): Flow<List<Message>>

    suspend fun enqueue(message: Message)

    suspend fun sendPending(): Result<Unit>
}

class FakeAuthRepository(private val userId: UUID = UUID.randomUUID()) : AuthRepository {
    private val sessionState = kotlinx.coroutines.flow.MutableStateFlow<UUID?>(null)
    override val session: Flow<UUID?> = sessionState

    override suspend fun signIn(
        email: String,
        password: String,
    ): Result<UUID> = Result.success(userId).also { sessionState.value = userId }

    override suspend fun signUp(
        email: String,
        password: String,
        displayName: String,
    ): Result<UUID> =
        Result.success(userId).also {
            sessionState.value = userId
        }

    override suspend fun signOut() {
        sessionState.value = null
    }
}

class LocalChatRepository(private val database: InMemoryPulseDatabase) : ChatRepository {
    override fun observeChats(): Flow<List<Chat>> = database.observeChats()

    override fun observeUsers(): Flow<List<User>> = database.observeUsers()

    override suspend fun createDirectChat(other: UUID): Result<UUID> {
        val id = UUID.nameUUIDFromBytes("direct:$other".toByteArray())
        database.saveChat(Chat(id, app.pulse.core.model.ChatType.DIRECT, "Direct chat", System.currentTimeMillis()))
        return Result.success(id)
    }
}

class LocalMessageRepository(private val database: InMemoryPulseDatabase, private val currentUser: UUID) : MessageRepository {
    override fun observeMessages(chatId: UUID): Flow<List<Message>> = database.observeMessages(chatId)

    override suspend fun enqueue(message: Message) =
        database.saveMessage(
            message.copy(senderId = currentUser, state = MessageState.PENDING),
        )

    override suspend fun sendPending(): Result<Unit> {
        database.pendingMessages().forEach { database.updateMessage(it.copy(state = MessageState.SENT)) }
        return Result.success(Unit)
    }
}
