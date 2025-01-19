package com.stark.shoot.application.port.`in`.chat

import com.stark.shoot.adapter.`in`.web.dto.ChatMessageRequest

interface SendMessageUseCase {
    fun handleMessage(message: ChatMessageRequest, userId: String?)
}