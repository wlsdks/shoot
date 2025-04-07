package com.stark.shoot.application.port.`in`.user

import com.stark.shoot.domain.chat.user.User

interface FindUserUseCase {
    fun findById(userId: Long): User?
    fun findByUsername(username: String): User?
    fun findByUserCode(userCode: String): User?
}