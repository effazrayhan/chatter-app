package app.pulse.feature.groups

import app.pulse.core.model.ChatType
import java.util.UUID

data class GroupDraft(
    val title: String,
    val description: String = "",
    val memberIds: List<UUID> = emptyList(),
    val channel: Boolean = false,
)

data class GroupMember(val userId: UUID, val role: Role)

enum class Role { OWNER, ADMIN, MEMBER }

fun canPost(
    type: ChatType,
    role: Role,
): Boolean = type != ChatType.CHANNEL || role == Role.OWNER || role == Role.ADMIN

fun inviteUri(code: String): String = "pulse://join/$code"
