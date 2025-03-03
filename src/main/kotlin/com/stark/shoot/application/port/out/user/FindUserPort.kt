package com.stark.shoot.application.port.out.user

import com.stark.shoot.domain.chat.user.User
import org.bson.types.ObjectId

interface FindUserPort {
    fun findByUsername(username: String): User?
    fun findUserById(id: ObjectId): User?
    fun findAll(): List<User>

    fun findByUserCode(userCode: String): User?
    fun findRandomUsers(excludeUserId: ObjectId, limit: Int): List<User>
    fun findByCode(newCode: String): User?
    fun findByUsernameOrUserCode(query: String): List<User>
}