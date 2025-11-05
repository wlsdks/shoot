package com.stark.shoot.application.port.`in`.user.friend.command

import com.stark.shoot.adapter.`in`.rest.dto.social.friend.AcceptFriendRequest
import com.stark.shoot.domain.shared.UserId

data class AcceptFriendRequestCommand(
    val currentUserId: UserId,
    val requesterId: UserId
) {

    companion object {
        fun of(request: AcceptFriendRequest): AcceptFriendRequestCommand {
            return AcceptFriendRequestCommand(
                currentUserId = UserId.from(request.userId),
                requesterId = UserId.from(request.requesterId)
            )
        }
    }

}