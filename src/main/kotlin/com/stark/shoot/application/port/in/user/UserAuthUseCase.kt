package com.stark.shoot.application.port.`in`.user

import com.stark.shoot.adapter.`in`.web.dto.user.UserResponse
import org.springframework.security.core.Authentication

interface UserAuthUseCase {
    fun retrieveUserDetails(authentication: Authentication?): UserResponse
}