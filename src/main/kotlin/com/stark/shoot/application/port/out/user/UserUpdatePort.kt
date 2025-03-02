package com.stark.shoot.application.port.out.user

import com.stark.shoot.domain.chat.user.User
import org.bson.types.ObjectId

interface UserUpdatePort {
    fun updateUser(user: User): User
    fun findUserById(userId: ObjectId): User
}