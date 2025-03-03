package com.stark.shoot.infrastructure.common.exception.socket

data class WebSocketError(
    val code: String,
    val message: String
)