package com.stark.shoot.application.port.`in`.user.group.command

/**
 * Command for deleting a friend group
 */
data class DeleteGroupCommand(
    val groupId: Long
) {
    companion object {
        fun of(groupId: Long): DeleteGroupCommand {
            return DeleteGroupCommand(groupId)
        }
    }
}