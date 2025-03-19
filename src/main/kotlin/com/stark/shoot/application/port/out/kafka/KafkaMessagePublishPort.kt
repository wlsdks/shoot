package com.stark.shoot.application.port.out.kafka

import com.stark.shoot.domain.chat.event.ChatEvent
import java.util.concurrent.CompletableFuture

interface KafkaMessagePublishPort {
    fun publishChatEvent(topic: String, key: String, event: ChatEvent): CompletableFuture<Void>
    suspend fun publishChatEventSuspend(topic: String, key: String, event: ChatEvent)
}