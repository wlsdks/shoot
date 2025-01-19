package com.stark.shoot.adapter.`in`.web.dto.user

import com.fasterxml.jackson.annotation.JsonCreator

data class LoginRequest @JsonCreator constructor(
    val username: String,
    val password: String
)