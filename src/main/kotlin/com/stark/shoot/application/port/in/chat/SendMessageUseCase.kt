package com.stark.shoot.application.port.`in`.chat

import com.stark.shoot.adapter.`in`.web.dto.ChatMessageRequest
import java.util.concurrent.CompletableFuture

interface SendMessageUseCase {
    fun handleMessage(message: ChatMessageRequest): CompletableFuture<Void>
}