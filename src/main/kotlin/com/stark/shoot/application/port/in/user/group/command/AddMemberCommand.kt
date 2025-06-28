package com.stark.shoot.application.port.`in`.user.group.command

import com.stark.shoot.domain.user.vo.UserId

/**
 * Command for adding a member to a friend group
 */
data class AddMemberCommand(
    val groupId: Long,
    val memberId: UserId
) {
    companion object {
        fun of(groupId: Long, memberId: UserId): AddMemberCommand {
            return AddMemberCommand(groupId, memberId)
        }
        
        fun of(groupId: Long, memberId: Long): AddMemberCommand {
            return AddMemberCommand(groupId, UserId.from(memberId))
        }
    }
}