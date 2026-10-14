package app.pulse.core.data

import app.pulse.core.database.InMemoryPulseDatabase
import app.pulse.core.model.Message
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class SyncEngine(private val database: InMemoryPulseDatabase) {
    fun observe(chatId: UUID): Flow<List<Message>> = database.observeMessages(chatId)

    suspend fun applyRemote(message: Message) = database.saveMessage(message.copy(state = app.pulse.core.model.MessageState.SENT))
}
