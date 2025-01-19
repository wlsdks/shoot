package com.stark.shoot.adapter.`in`.web.dto.user

import com.fasterxml.jackson.annotation.JsonCreator

data class UserResponse @JsonCreator constructor(
    val id: String,
    val username: String,
    val nickname: String
)
