package com.stark.shoot.application.port.`in`.user.profile

import com.stark.shoot.adapter.`in`.web.dto.user.SetBackgroundImageRequest
import com.stark.shoot.adapter.`in`.web.dto.user.SetProfileImageRequest
import com.stark.shoot.adapter.`in`.web.dto.user.UpdateProfileRequest
import com.stark.shoot.domain.chat.user.User

interface UserUpdateProfileUseCase {
    fun updateProfile(userId: Long, request: UpdateProfileRequest): User
    fun setProfileImage(userId: Long, request: SetProfileImageRequest): User
    fun setBackgroundImage(userId: Long, request: SetBackgroundImageRequest): User
}
