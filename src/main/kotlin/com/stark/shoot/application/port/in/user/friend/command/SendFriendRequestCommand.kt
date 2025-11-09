package com.stark.shoot.application.port.`in`.user.friend.command

import com.stark.shoot.adapter.`in`.rest.dto.social.friend.SendFriendRequest
import com.stark.shoot.domain.shared.UserId

data class SendFriendRequestCommand(
    val currentUserId: UserId,
    val targetUserId: UserId
) {

    companion object {
        fun of(request: SendFriendRequest): SendFriendRequestCommand {
            return SendFriendRequestCommand(
                currentUserId = UserId.from(request.userId),
                targetUserId = UserId.from(request.targetUserId)
            )
        }
    }

}