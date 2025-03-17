package com.stark.shoot.infrastructure.exception.web

import java.time.Instant

data class ErrorResponse(
    val status: Int,
    val message: String,
    val timestamp: Long
)
