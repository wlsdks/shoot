package com.stark.shoot.infrastructure.exception.socket

data class WebSocketError(
    val code: String,
    val message: String
)