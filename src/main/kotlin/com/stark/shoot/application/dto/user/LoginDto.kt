package com.stark.shoot.application.dto.user

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
data class LoginResponse(
    val userId: String,
    val accessToken: String,
    val refreshToken: String
)
