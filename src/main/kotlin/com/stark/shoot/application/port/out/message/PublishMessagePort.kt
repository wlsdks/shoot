package com.stark.shoot.application.port.out.message

import com.stark.shoot.adapter.`in`.rest.dto.message.ChatMessageRequest

interface PublishMessagePort {
    suspend fun publish(message: ChatMessageRequest)
}
