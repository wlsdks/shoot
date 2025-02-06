package com.stark.shoot.adapter.`in`.kafka

import com.stark.shoot.application.port.`in`.message.ProcessMessageUseCase
import com.stark.shoot.domain.chat.event.ChatEvent
import com.stark.shoot.domain.chat.event.EventType
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.kafka.annotation.DltHandler
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.messaging.handler.annotation.Header
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Component

/**
 * 채팅 메시지 이벤트를 수신하여 처리합니다.
 */
@Component
class ChatMessageConsumer(
    private val processMessageUseCase: ProcessMessageUseCase
) {
    private val logger = KotlinLogging.logger {}

    @KafkaListener(
        topics = ["chat-messages"],
        groupId = "chat-group",
        errorHandler = "chatMessageErrorHandler"
    )
    fun consumeMessage(
        @Payload event: ChatEvent,
        @Header("correlationId") correlationId: String
    ) {
        when (event.type) {
            EventType.MESSAGE_CREATED -> {
                try {
                    // MongoDB에 저장하고 채팅방 메타데이터 업데이트
                    processMessageUseCase.processMessage(event.data)
                } catch (e: Exception) {
                    logger.error(e) { "Failed to process message: ${event.data}" }
                }
            }
            // 다른 이벤트 타입은 무시
            else -> {}
        }
    }

    @DltHandler
    fun handleDlt(event: ChatEvent) {
        logger.error { "Failed to process message: ${event.data}" }
    }

}