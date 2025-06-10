package com.stark.shoot.adapter.`in`.web.dto.user

import com.stark.shoot.infrastructure.annotation.ApplicationDto

/**
 * 사용자 로그인 요청
 */
data class LoginRequest(
    val username: String,
    val password: String
)

/**
 * 로그인 응답
 */
@ApplicationDto
data class LoginResponse(
    val userId: String,
    val accessToken: String,
    val refreshToken: String
)
