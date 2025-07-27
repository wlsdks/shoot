package com.stark.shoot.application.port.`in`.user.friend.command

import com.stark.shoot.adapter.`in`.rest.dto.social.friend.RejectFriendRequest
import com.stark.shoot.domain.user.vo.UserId

data class RejectFriendRequestCommand(
    val currentUserId: UserId,
    val requesterId: UserId
) {

    companion object {
        fun of(request: RejectFriendRequest): RejectFriendRequestCommand {
            return RejectFriendRequestCommand(
                currentUserId = UserId.from(request.userId),
                requesterId = UserId.from(request.requesterId)
            )
        }
    }

}