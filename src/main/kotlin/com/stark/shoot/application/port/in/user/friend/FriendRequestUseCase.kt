package com.stark.shoot.application.port.`in`.user.friend

import com.stark.shoot.application.port.`in`.user.friend.command.CancelFriendRequestCommand
import com.stark.shoot.application.port.`in`.user.friend.command.SendFriendRequestCommand
import com.stark.shoot.application.port.`in`.user.friend.command.SendFriendRequestFromCodeCommand

interface FriendRequestUseCase {
    fun sendFriendRequest(command: SendFriendRequestCommand)
    fun cancelFriendRequest(command: CancelFriendRequestCommand)

    // todo: 이 메서드는 확인이 필요함 첫번째랑 기능상 중복인가..?
    fun sendFriendRequestFromUserCode(command: SendFriendRequestFromCodeCommand)
}
