package com.stark.shoot.infrastructure.exception.web

import com.stark.shoot.infrastructure.annotation.ApplicationDto

@ApplicationDto
data class ErrorResponse(
    val status: Int,
    val message: String,
    val timestamp: Long
)
