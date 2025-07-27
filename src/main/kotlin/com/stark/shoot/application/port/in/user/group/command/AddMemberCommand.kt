package com.stark.shoot.application.port.`in`.user.group.command

import com.stark.shoot.adapter.`in`.rest.dto.social.group.AddMemberInGroupRequest
import com.stark.shoot.domain.user.vo.UserId

/**
 * Command for adding a member to a friend group
 */
data class AddMemberCommand(
    val groupId: Long,
    val memberId: UserId
) {
    companion object {
        fun of(request: AddMemberInGroupRequest): AddMemberCommand {
            return AddMemberCommand(
                request.groupId,
                UserId.from(request.memberId)
            )
        }

        fun of(groupId: Long, memberId: Long): AddMemberCommand {
            return AddMemberCommand(groupId, UserId.from(memberId))
        }
    }
}