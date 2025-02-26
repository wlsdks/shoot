package com.stark.shoot.adapter.`in`.kafka

import com.stark.shoot.application.port.`in`.message.ProcessMessageUseCase
import com.stark.shoot.domain.chat.event.ChatEvent
import com.stark.shoot.domain.chat.event.EventType
import com.stark.shoot.domain.chat.message.ChatMessage
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Component
import java.util.concurrent.CompletableFuture

@Component
class MessageKafkaConsumer(
    private val processMessageUseCase: ProcessMessageUseCase,
    private val redisTemplate: RedisTemplate<String, ChatMessage>,
    private val messagingTemplate: SimpMessagingTemplate
) {

    private val logger = KotlinLogging.logger {}

    @KafkaListener(topics = ["chat-messages"], groupId = "shoot-\${random.uuid}")
    fun consumeMessage(@Payload event: ChatEvent) {
        if (event.type == EventType.MESSAGE_CREATED) {
            // 메시지 저장
            val savedMessage = processMessageUseCase.processMessage(event.data)

            // 메시지를 WebSocket으로 전송
            CompletableFuture.runAsync {
                logger.debug { "Broadcasting message: $savedMessage" }
                messagingTemplate.convertAndSend("/topic/messages/${savedMessage.roomId}", savedMessage)
                logger.info { "Message pushed to /topic/messages/${savedMessage.roomId}: ${savedMessage.content.text}" }
            }

            // 메시지를 Redis에 저장
            CompletableFuture.runAsync {
                redisTemplate.opsForList().leftPush("chat:${savedMessage.roomId}:messages", savedMessage)
                redisTemplate.opsForList().trim("chat:${savedMessage.roomId}:messages", 0, 9)
            }
        }
    }

}