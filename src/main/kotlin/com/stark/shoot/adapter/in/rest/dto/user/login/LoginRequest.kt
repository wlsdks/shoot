package com.stark.shoot.adapter.`in`.rest.dto.user.login

/**
 * 사용자 로그인 요청
 */
data class LoginRequest(
    val username: String,
    val password: String
)