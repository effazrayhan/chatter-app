package app.pulse.core.network

import app.pulse.core.model.Message
import app.pulse.core.model.User
import java.util.UUID

interface SupabaseRemoteDataSource {
    suspend fun signIn(
        email: String,
        password: String,
    ): Result<UUID>

    suspend fun signUp(
        email: String,
        password: String,
        displayName: String,
    ): Result<UUID>

    suspend fun searchUsers(query: String): Result<List<User>>

    suspend fun createDirectChat(other: UUID): Result<UUID>

    suspend fun insertMessage(message: Message): Result<Message>

    suspend fun catchUp(
        chatId: UUID,
        afterSeq: Long,
    ): Result<List<Message>>
}

class UnconfiguredSupabaseRemoteDataSource : SupabaseRemoteDataSource {
    private val error = IllegalStateException("Supabase is not configured; set local.properties values")

    override suspend fun signIn(
        email: String,
        password: String,
    ) = Result.failure<UUID>(error)

    override suspend fun signUp(
        email: String,
        password: String,
        displayName: String,
    ) = Result.failure<UUID>(error)

    override suspend fun searchUsers(query: String) = Result.failure<List<User>>(error)

    override suspend fun createDirectChat(other: UUID) = Result.failure<UUID>(error)

    override suspend fun insertMessage(message: Message) = Result.failure<Message>(error)

    override suspend fun catchUp(
        chatId: UUID,
        afterSeq: Long,
    ) = Result.failure<List<Message>>(error)
}
