package com.stark.shoot.application.port.`in`.user.friend.command

import com.stark.shoot.adapter.`in`.rest.dto.social.code.SendFriendRequestByCodeRequest
import com.stark.shoot.domain.shared.UserId

data class SendFriendRequestFromCodeCommand(
    val currentUserId: UserId,
    val targetUserId: UserId
) {

    companion object {
        fun of(request: SendFriendRequestByCodeRequest, targetUserId: UserId): SendFriendRequestFromCodeCommand {
            return SendFriendRequestFromCodeCommand(
                currentUserId = UserId.from(request.userId),
                targetUserId = targetUserId
            )
        }
    }

}