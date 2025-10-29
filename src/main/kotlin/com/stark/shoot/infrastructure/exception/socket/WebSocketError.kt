package com.stark.shoot.domain.exception.socket

data class WebSocketError(
    val code: String,
    val message: String
)