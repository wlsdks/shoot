package com.stark.shoot.application.port.`in`.user.auth

import com.stark.shoot.adapter.`in`.web.dto.user.UserResponse
import com.stark.shoot.application.port.`in`.user.auth.command.RetrieveUserDetailsCommand

interface UserAuthUseCase {
    fun retrieveUserDetails(command: RetrieveUserDetailsCommand): UserResponse
}
