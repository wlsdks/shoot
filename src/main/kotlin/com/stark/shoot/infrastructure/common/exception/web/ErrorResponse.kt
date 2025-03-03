package com.stark.shoot.infrastructure.common.exception.web

data class ErrorResponse(
    val status: Int,
    val message: String,
    val timestamp: Long
)
