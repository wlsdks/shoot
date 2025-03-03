package com.stark.shoot.application.port.`in`.user

import com.stark.shoot.adapter.`in`.web.dto.user.UpdateProfileRequest
import com.stark.shoot.domain.chat.user.User
import org.bson.types.ObjectId

interface UserUpdateProfileUseCase {
    fun updateProfile(userId: ObjectId, request: UpdateProfileRequest): User
}