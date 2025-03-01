package com.stark.shoot.adapter.`in`.web.dto.user

data class LoginResponse(
    val userId: String,
    val accessToken: String,
    val refreshToken: String
)