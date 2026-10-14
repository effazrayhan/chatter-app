package app.pulse.core

import app.pulse.core.data.AuthRepository
import app.pulse.core.data.FakeAuthRepository
import app.pulse.core.data.LocalChatRepository
import app.pulse.core.data.LocalMessageRepository
import app.pulse.core.database.InMemoryPulseDatabase
import java.util.UUID

object AppContainer {
    val database = InMemoryPulseDatabase()
    val authRepository: AuthRepository = FakeAuthRepository()
    val chatRepository = LocalChatRepository(database)
    val currentUserId: UUID = UUID.nameUUIDFromBytes("local-user".toByteArray())
    val messageRepository = LocalMessageRepository(database, currentUserId)
}
