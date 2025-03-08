package com.stark.shoot.application.port.`in`.message

import com.stark.shoot.adapter.`in`.web.dto.message.ChatMessageRequest
import java.util.concurrent.CompletableFuture

interface SendMessageUseCase {
    fun handleMessage(requestMessage: ChatMessageRequest): CompletableFuture<String?>
}