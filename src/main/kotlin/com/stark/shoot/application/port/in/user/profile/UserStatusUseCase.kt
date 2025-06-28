package com.stark.shoot.application.port.`in`.user.profile

import com.stark.shoot.application.port.`in`.user.profile.command.UpdateUserStatusCommand
import com.stark.shoot.domain.user.User

interface UserStatusUseCase {
    fun updateStatus(command: UpdateUserStatusCommand): User
}
