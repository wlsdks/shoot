package com.stark.shoot.adapter.`in`.rest.dto.user.login

import com.stark.shoot.infrastructure.annotation.ApplicationDto

/**
 * 로그인 응답
 */
@ApplicationDto
data class LoginResponse(
    val userId: String,
    val accessToken: String,
    val refreshToken: String
)