package com.stark.shoot.application.port.`in`.active

import com.stark.shoot.application.port.`in`.active.command.UserActivityCommand

interface UserActiveUseCase {
    fun updateUserActive(command: UserActivityCommand)
}
