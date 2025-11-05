package com.stark.shoot.application.port.out.kafka

import com.stark.shoot.domain.shared.event.MessageEvent
import java.util.concurrent.CompletableFuture

interface PublishKafkaPort {
    fun publishChatEvent(topic: String, key: String, event: MessageEvent): CompletableFuture<Void>
    suspend fun publishChatEventSuspend(topic: String, key: String, event: MessageEvent)
}