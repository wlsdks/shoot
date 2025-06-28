package com.stark.shoot.application.port.`in`.user.group.command

import com.stark.shoot.domain.user.vo.UserId

/**
 * Command for removing a member from a friend group
 */
data class RemoveMemberCommand(
    val groupId: Long,
    val memberId: UserId
) {
    companion object {
        fun of(groupId: Long, memberId: UserId): RemoveMemberCommand {
            return RemoveMemberCommand(groupId, memberId)
        }
        
        fun of(groupId: Long, memberId: Long): RemoveMemberCommand {
            return RemoveMemberCommand(groupId, UserId.from(memberId))
        }
    }
}