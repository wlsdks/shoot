package com.stark.shoot.application.port.`in`.user.friend

import com.stark.shoot.application.port.`in`.user.friend.command.RemoveFriendCommand
import com.stark.shoot.domain.user.User

interface FriendRemoveUseCase {
    fun removeFriend(command: RemoveFriendCommand): User
}
