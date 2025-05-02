package com.stark.shoot.adapter.`in`.web.dto.message.read

data class ChatReadRequest(
    val roomId: Long,
    val userId: Long,
    val messageId: String? = null,
    val requestId: String? = null
)