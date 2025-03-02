package com.stark.shoot.application.port.`in`.user

import com.stark.shoot.domain.chat.user.User
import org.bson.types.ObjectId

interface UserUpdateProfileUseCase {
    fun updateProfile(userId: ObjectId, nickname: String?, profileImageUrl: String?, bio: String?): User
}