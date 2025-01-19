package com.stark.shoot.adapter.`in`.web.dto.user

import com.fasterxml.jackson.annotation.JsonCreator

data class LoginResponse @JsonCreator constructor(
    val userId: String,
    val accessToken: String
)