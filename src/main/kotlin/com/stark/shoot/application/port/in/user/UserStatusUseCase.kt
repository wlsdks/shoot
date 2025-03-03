package com.stark.shoot.application.port.`in`.user

import com.stark.shoot.adapter.out.persistence.mongodb.document.user.type.UserStatus
import com.stark.shoot.domain.chat.user.User
import org.bson.types.ObjectId
import org.springframework.security.core.Authentication

interface UserStatusUseCase {
    fun updateStatus(authentication: Authentication, status: UserStatus): User
}