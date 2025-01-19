package com.stark.shoot.infrastructure.common.exception

data class WebSocketError(
    val code: String,
    val message: String
)