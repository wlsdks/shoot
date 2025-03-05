package com.stark.shoot.infrastructure.exception.web

data class ErrorResponse(
    val status: Int,
    val message: String,
    val timestamp: Long
)
