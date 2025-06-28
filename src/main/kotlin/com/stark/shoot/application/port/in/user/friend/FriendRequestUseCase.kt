package com.stark.shoot.application.port.`in`.user.friend

import com.stark.shoot.application.port.`in`.user.friend.command.CancelFriendRequestCommand
import com.stark.shoot.application.port.`in`.user.friend.command.SendFriendRequestCommand

interface FriendRequestUseCase {
    fun sendFriendRequest(command: SendFriendRequestCommand)
    fun cancelFriendRequest(command: CancelFriendRequestCommand)
}
