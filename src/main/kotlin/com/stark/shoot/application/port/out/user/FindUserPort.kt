package com.stark.shoot.application.port.out.user

import com.stark.shoot.domain.chat.user.User

interface FindUserPort {
    fun findByUsername(username: String): User?
    fun findUserById(userId: Long): User?
    fun findAll(): List<User>

    fun findByUserCode(userCode: String): User?
    fun findRandomUsers(excludeUserId: Long, limit: Int): List<User>
}