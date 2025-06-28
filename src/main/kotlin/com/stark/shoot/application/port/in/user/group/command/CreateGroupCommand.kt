package com.stark.shoot.application.port.`in`.user.group.command

import com.stark.shoot.domain.user.vo.UserId

/**
 * Command for creating a new friend group
 */
data class CreateGroupCommand(
    val ownerId: UserId,
    val name: String,
    val description: String?
) {
    companion object {
        fun of(ownerId: UserId, name: String, description: String?): CreateGroupCommand {
            return CreateGroupCommand(ownerId, name, description)
        }
        
        fun of(ownerId: Long, name: String, description: String?): CreateGroupCommand {
            return CreateGroupCommand(UserId.from(ownerId), name, description)
        }
    }
}