package com.stark.shoot.adapter.`in`.rest.dto.user

data class UpdateProfileRequest(
    val nickname: String?,
    val profileImageUrl: String?,
    val backgroundImageUrl: String?,
    val bio: String?
)