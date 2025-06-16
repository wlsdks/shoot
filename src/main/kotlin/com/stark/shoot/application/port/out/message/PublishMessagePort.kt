package com.stark.shoot.application.port.out.message

import com.stark.shoot.adapter.`in`.web.dto.message.ChatMessageRequest

interface PublishMessagePort {
    suspend fun publish(message: ChatMessageRequest)
}
