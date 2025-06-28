package com.stark.shoot.application.port.`in`.user.group.command

/**
 * Command for getting a friend group by ID
 */
data class GetGroupCommand(
    val groupId: Long
) {
    companion object {
        fun of(groupId: Long): GetGroupCommand {
            return GetGroupCommand(groupId)
        }
    }
}