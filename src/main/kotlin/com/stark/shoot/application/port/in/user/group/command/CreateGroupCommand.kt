package com.stark.shoot.application.port.`in`.user.group.command

import com.stark.shoot.adapter.`in`.rest.dto.social.group.CreateGroupRequest
import com.stark.shoot.domain.shared.UserId

/**
 * Command for creating a new friend group
 */
data class CreateGroupCommand(
    val ownerId: UserId,
    val name: String,
    val description: String?
) {
    companion object {
        fun of(request: CreateGroupRequest): CreateGroupCommand {
            return CreateGroupCommand(
                UserId.from(request.ownerId),
                request.name,
                request.description
            )
        }

        fun of(ownerId: Long, name: String, description: String?): CreateGroupCommand {
            return CreateGroupCommand(UserId.from(ownerId), name, description)
        }
    }
}