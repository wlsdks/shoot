package com.stark.shoot.adapter.`in`.web.dto.user

data class UserResponse(
    val id: String,
    val username: String,
    val nickname: String,
    val userCode: String? = null
)
