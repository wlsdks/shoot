package com.stark.shoot.infrastructure.common.exception

data class ErrorResponse(
    val status: Int,
    val message: String,
    val timestamp: Long
)
