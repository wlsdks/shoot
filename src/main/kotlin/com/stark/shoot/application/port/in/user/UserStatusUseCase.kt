package com.stark.shoot.application.port.`in`.user

import com.stark.shoot.adapter.out.persistence.mongodb.document.user.type.UserStatus
import com.stark.shoot.domain.chat.user.User
import org.bson.types.ObjectId

interface UserStatusUseCase {
    fun updateStatus(userId: ObjectId, status: UserStatus): User
}