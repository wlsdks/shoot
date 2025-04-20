package com.stark.shoot.application.port.`in`.message

import com.stark.shoot.adapter.`in`.web.dto.message.ChatMessageRequest

interface SendMessageUseCase {
    fun sendMessage(message: ChatMessageRequest)
}