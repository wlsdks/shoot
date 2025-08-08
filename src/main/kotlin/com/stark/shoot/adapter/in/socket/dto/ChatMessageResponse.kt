package com.stark.shoot.adapter.`in`.socket.dto

import com.stark.shoot.infrastructure.annotation.ApplicationDto

@ApplicationDto
data class ChatMessageResponse(
    val status: String,
    val content: String
)