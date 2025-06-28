package com.stark.shoot.application.port.`in`.user.group.command

import com.stark.shoot.domain.user.vo.UserId

/**
 * Command for getting all friend groups for a specific owner
 */
data class GetGroupsCommand(
    val ownerId: UserId
) {
    companion object {
        fun of(ownerId: UserId): GetGroupsCommand {
            return GetGroupsCommand(ownerId)
        }
        
        fun of(ownerId: Long): GetGroupsCommand {
            return GetGroupsCommand(UserId.from(ownerId))
        }
    }
}