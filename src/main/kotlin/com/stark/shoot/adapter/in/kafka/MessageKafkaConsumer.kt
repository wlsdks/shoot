package com.stark.shoot.adapter.`in`.kafka

import com.stark.shoot.application.port.`in`.message.ProcessMessageUseCase
import com.stark.shoot.domain.chat.event.ChatEvent
import com.stark.shoot.domain.chat.event.EventType
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Component
import java.util.concurrent.CompletableFuture

@Component
class MessageKafkaConsumer(
    private val processMessageUseCase: ProcessMessageUseCase,
    private val messagingTemplate: SimpMessagingTemplate
) {
    private val logger = KotlinLogging.logger {}

    @KafkaListener(topics = ["chat-messages"], groupId = "shoot-\${random.uuid}")
    fun consumeMessage(@Payload event: ChatEvent) {
        if (event.type == EventType.MESSAGE_CREATED) {
            try {
                // 1. 메시지 저장 (MongoDB에 영구 저장)
                val savedMessage = processMessageUseCase.processMessage(event.data)

                // 2. 메시지를 WebSocket으로 전송
                CompletableFuture.runAsync {
                    try {
                        messagingTemplate.convertAndSend("/topic/messages/${savedMessage.roomId}", savedMessage)
                    } catch (e: Exception) {
                        logger.error(e) { "Failed to send message through WebSocket: ${e.message}" }
                    }
                }
            } catch (e: Exception) {
                logger.error(e) { "Error processing message: ${e.message}" }
            }
        }
    }

}