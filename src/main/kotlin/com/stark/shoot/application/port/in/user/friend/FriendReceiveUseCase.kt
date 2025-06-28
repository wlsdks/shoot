package com.stark.shoot.application.port.`in`.user.friend

import com.stark.shoot.application.port.`in`.user.friend.command.AcceptFriendRequestCommand
import com.stark.shoot.application.port.`in`.user.friend.command.RejectFriendRequestCommand

interface FriendReceiveUseCase {
    /**
     * 친구 요청을 수락합니다.
     *
     * @param command 친구 요청 수락 커맨드
     */
    fun acceptFriendRequest(command: AcceptFriendRequestCommand)

    /**
     * 친구 요청을 거절합니다.
     *
     * @param command 친구 요청 거절 커맨드
     */
    fun rejectFriendRequest(command: RejectFriendRequestCommand)
}
