package com.stark.shoot.application.port.out.user

import com.stark.shoot.domain.chat.user.User
import org.bson.types.ObjectId

interface RetrieveUserPort {
    fun findByUsername(username: String): User?
    fun findById(id: ObjectId): User?
    fun findAll(): List<User>

    fun findByUserCode(userCode: String): User?
    fun findRandomUsers(excludeUserId: ObjectId, limit: Int): List<User>
}