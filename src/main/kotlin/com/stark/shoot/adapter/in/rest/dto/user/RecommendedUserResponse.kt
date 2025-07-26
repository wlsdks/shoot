package com.stark.shoot.adapter.`in`.rest.dto.user

import com.stark.shoot.infrastructure.annotation.ApplicationDto

@ApplicationDto
data class RecommendedUserResponse(
    val id: String,
    val username: String,
    val nickname: String
)