package com.stark.shoot.application.port.`in`.user.friend.command

import com.stark.shoot.adapter.`in`.rest.dto.social.friend.CancelFriendRequest
import com.stark.shoot.domain.shared.UserId

data class CancelFriendRequestCommand(
    val currentUserId: UserId,
    val targetUserId: UserId
) {

    companion object {
        fun of(request: CancelFriendRequest): CancelFriendRequestCommand {
            return CancelFriendRequestCommand(
                currentUserId = UserId.from(request.userId),
                targetUserId = UserId.from(request.targetUserId)
            )
        }
    }

}