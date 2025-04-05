package com.stark.shoot.application.port.`in`.user

import com.stark.shoot.domain.chat.user.User
import org.bson.types.ObjectId

interface FindUserUseCase {
    fun findById(id: ObjectId): User?
    fun findByUsername(username: String): User?
    fun findByUserCode(userCode: String): User?
}