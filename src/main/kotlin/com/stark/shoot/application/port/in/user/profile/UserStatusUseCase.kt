package com.stark.shoot.application.port.`in`.user.profile

import com.stark.shoot.domain.user.User
import com.stark.shoot.domain.user.type.UserStatus
import org.springframework.security.core.Authentication

interface UserStatusUseCase {
    fun updateStatus(authentication: Authentication, status: UserStatus): User
}