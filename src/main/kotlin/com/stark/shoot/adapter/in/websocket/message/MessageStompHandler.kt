package com.stark.shoot.adapter.`in`.websocket.message

import com.fasterxml.jackson.databind.ObjectMapper
import com.stark.shoot.adapter.`in`.web.dto.message.ChatMessageRequest
import com.stark.shoot.adapter.`in`.web.dto.message.MessageStatusResponse
import com.stark.shoot.application.port.`in`.message.SendMessageUseCase
import com.stark.shoot.infrastructure.common.exception.ErrorResponse
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Controller
import java.util.*
import java.util.concurrent.CompletableFuture

@Controller
class MessageStompHandler(
    private val sendMessageUseCase: SendMessageUseCase,
    private val messagingTemplate: SimpMessagingTemplate,
    private val redisTemplate: RedisTemplate<String, Any>,
    private val objectMapper: ObjectMapper
) {
    private val logger = KotlinLogging.logger {}

    /**
     * 클라이언트로부터 메시지를 수신하여 처리합니다.
     * 1. 메시지에 임시 ID와 "sending" 상태 추가
     * 2. Redis를 통해 메시지 즉시 브로드캐스트 (실시간성)
     * 3. Kafka를 통해 메시지 영속화 (안정성)
     * 4. 메시지 상태 업데이트를 클라이언트에 전송
     */
    @MessageMapping("/chat")
    fun handleChatMessage(message: ChatMessageRequest) {
        try {
            // 1. 메시지에 임시 ID 추가
            val tempId = UUID.randomUUID().toString()
            message.tempId = tempId
            message.status = "sending"

            // 메타데이터에 tempId 저장 (Kafka 처리 시 사용)
            message.metadata["tempId"] = tempId

            // 2. Redis로 즉시 메시지 발행 (실시간성)
            publishToRedis(message)

            // 3. Kafka로 메시지 발행 (지속성)
            sendToKafka(message)
                .exceptionally { throwable ->
                    // 오류 발생 시 처리
                    handleMessageError(message, tempId, throwable)
                    null
                }
        } catch (e: Exception) {
            logger.error(e) { "메시지 처리 중 예외 발생: ${e.message}" }
            val errorResponse = ErrorResponse(
                status = 500,
                message = e.message ?: "메시지 처리 중 오류가 발생했습니다",
                timestamp = System.currentTimeMillis()
            )
            messagingTemplate.convertAndSend("/topic/errors/${message.roomId}", errorResponse)
        }
    }

    /**
     * Redis를 통해 메시지를 실시간으로 발행합니다.
     */
    private fun publishToRedis(message: ChatMessageRequest) {
        val channel = "chat:room:${message.roomId}"
        try {
            redisTemplate.convertAndSend(channel, objectMapper.writeValueAsString(message))
            logger.debug { "Redis 채널에 메시지 발행: $channel, tempId: ${message.tempId}" }
        } catch (e: Exception) {
            logger.error(e) { "Redis 발행 실패: ${e.message}" }
            throw e // 호출자가 처리할 수 있도록 예외 전파
        }
    }

    /**
     * Kafka를 통해 메시지를 발행하고 상태를 업데이트합니다.
     */
    private fun sendToKafka(message: ChatMessageRequest): CompletableFuture<Void> {
        val tempId = message.tempId ?: ""

        return sendMessageUseCase.handleMessage(message)
            .thenAccept { _ ->
                // Kafka 발행 성공 시 상태 업데이트
                val statusUpdate = MessageStatusResponse(
                    tempId = tempId,
                    status = "sent_to_kafka", // Kafka로 전송됨 (아직 DB에 저장되지 않음)
                    persistedId = null,
                    errorMessage = null
                )
                messagingTemplate.convertAndSend("/topic/message/status/${message.roomId}", statusUpdate)
                logger.debug { "메시지 Kafka 발행 성공, 상태 업데이트: tempId=$tempId" }
            }
    }

    /**
     * 메시지 처리 중 오류 발생 시 클라이언트에 상태 업데이트 전송
     */
    private fun handleMessageError(message: ChatMessageRequest, tempId: String, throwable: Throwable) {
        logger.error(throwable) { "Kafka 발행 실패: ${message.content.text}" }

        // 1. 일반 오류 채널로 전송
        val errorResponse = ErrorResponse(
            status = 500,
            message = throwable.message ?: "메시지 처리 중 오류가 발생했습니다",
            timestamp = System.currentTimeMillis()
        )
        messagingTemplate.convertAndSend("/topic/errors/${message.roomId}", errorResponse)

        // 2. 메시지 상태 업데이트 전송
        val statusUpdate = MessageStatusResponse(
            tempId = tempId,
            status = "failed",
            persistedId = null,
            errorMessage = throwable.message
        )
        messagingTemplate.convertAndSend("/topic/message/status/${message.roomId}", statusUpdate)
    }

}