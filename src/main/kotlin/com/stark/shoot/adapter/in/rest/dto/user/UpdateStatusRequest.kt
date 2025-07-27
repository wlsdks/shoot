package com.stark.shoot.adapter.`in`.rest.dto.user

import com.stark.shoot.domain.user.type.UserStatus

data class UpdateStatusRequest(
    val userId: String,
    val status: UserStatus
)