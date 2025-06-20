package com.stark.shoot.application.port.`in`.user.profile

import com.stark.shoot.adapter.`in`.web.dto.user.SetBackgroundImageRequest
import com.stark.shoot.adapter.`in`.web.dto.user.SetProfileImageRequest
import com.stark.shoot.adapter.`in`.web.dto.user.UpdateProfileRequest
import com.stark.shoot.domain.user.User
import com.stark.shoot.domain.user.vo.UserId

interface UserUpdateProfileUseCase {
    fun updateProfile(userId: UserId, request: UpdateProfileRequest): User
    fun setProfileImage(userId: UserId, request: SetProfileImageRequest): User
    fun setBackgroundImage(userId: UserId, request: SetBackgroundImageRequest): User
}
