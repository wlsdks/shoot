package com.stark.shoot.application.port.`in`.user.group.command

/**
 * Command for renaming an existing friend group
 */
data class RenameGroupCommand(
    val groupId: Long,
    val newName: String
) {
    companion object {
        fun of(groupId: Long, newName: String): RenameGroupCommand {
            return RenameGroupCommand(groupId, newName)
        }
    }
}