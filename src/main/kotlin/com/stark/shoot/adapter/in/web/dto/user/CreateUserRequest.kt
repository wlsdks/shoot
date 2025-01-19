package com.stark.shoot.adapter.`in`.web.dto.user

import com.fasterxml.jackson.annotation.JsonCreator

data class CreateUserRequest @JsonCreator constructor(
    val username: String,
    val nickname: String
)
