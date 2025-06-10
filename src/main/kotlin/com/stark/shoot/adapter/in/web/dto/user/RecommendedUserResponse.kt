package com.stark.shoot.adapter.`in`.web.dto.user

import com.stark.shoot.infrastructure.annotation.ApplicationDto

@ApplicationDto
data class RecommendedUserResponse(
    val id: String,
    val username: String,
    val nickname: String
)