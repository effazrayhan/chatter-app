package app.pulse.feature.chat

import app.pulse.core.model.Message
import java.util.UUID

data class Reaction(val messageId: UUID, val userId: UUID, val emoji: String)

fun Message.reply(replyToId: UUID): Message = copy(replyToId = replyToId)

fun Message.edit(
    body: String,
    now: Long = System.currentTimeMillis(),
): Message = copy(body = body.take(4096), editedAt = now)

fun Message.deleteForEveryone(now: Long = System.currentTimeMillis()): Message = copy(body = "This message was deleted", deletedAt = now)

fun toggleReaction(
    reactions: Set<Reaction>,
    reaction: Reaction,
): Set<Reaction> = if (reaction in reactions) reactions - reaction else reactions + reaction

fun searchMessages(
    messages: List<Message>,
    query: String,
): List<Message> = messages.filter { query.isBlank() || it.body.contains(query, ignoreCase = true) }
