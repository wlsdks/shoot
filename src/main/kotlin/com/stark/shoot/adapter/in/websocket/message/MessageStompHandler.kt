package com.stark.shoot.adapter.`in`.websocket.message

import com.fasterxml.jackson.databind.ObjectMapper
import com.stark.shoot.adapter.`in`.web.dto.message.ChatMessageRequest
import com.stark.shoot.application.port.`in`.message.SendMessageUseCase
import com.stark.shoot.infrastructure.common.exception.ErrorResponse
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Controller

@Controller
class MessageStompHandler(
    private val sendMessageUseCase: SendMessageUseCase,
    private val messagingTemplate: SimpMessagingTemplate,
    private val redisTemplate: RedisTemplate<String, Any>,
    private val objectMapper: ObjectMapper
) {

    private val logger = KotlinLogging.logger {}

    // 클라이언트가 /app/chat로 메시지를 보내면 실시간으로 Redis로 발행하고,
    // 동시에 Kafka로도 발행하여 영속성 보장
    @MessageMapping("/chat")
    fun handleChatMessage(message: ChatMessageRequest) {
        try {
            // 1. Redis로 즉시 메시지 발행 (실시간성 향상)
            val channel = "chat:room:${message.roomId}"
            redisTemplate.convertAndSend(channel, objectMapper.writeValueAsString(message))
            logger.debug { "Message published to Redis channel: $channel" }

            // 2. Kafka로도 메시지 발행 (지속성 보장)
            sendMessageUseCase.handleMessage(message)
                .whenComplete { _, throwable ->
                    if (throwable != null) {
                        logger.error(throwable) { "Failed to publish message to Kafka: ${message.content.text}" }
                        val errorResponse = ErrorResponse(
                            status = 500,
                            message = throwable.message ?: "메시지 처리 중 오류가 발생했습니다.",
                            timestamp = System.currentTimeMillis()
                        )
                        messagingTemplate.convertAndSend("/topic/errors/${message.roomId}", errorResponse)
                    } else {
                        logger.debug { "Message successfully published to Kafka: ${message.content.text}" }
                    }
                }
        } catch (e: Exception) {
            logger.error(e) { "Error processing message: ${message.content.text}" }
            val errorResponse = ErrorResponse(
                status = 500,
                message = e.message ?: "메시지 처리 중 오류가 발생했습니다.",
                timestamp = System.currentTimeMillis()
            )
            messagingTemplate.convertAndSend("/topic/errors/${message.roomId}", errorResponse)
        }
    }

}