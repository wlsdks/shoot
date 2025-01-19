package com.stark.shoot.application.service.chat

import com.stark.shoot.adapter.`in`.web.dto.ChatMessageRequest
import com.stark.shoot.adapter.out.persistence.mongodb.document.message.embedded.type.MessageStatus
import com.stark.shoot.adapter.out.persistence.mongodb.document.message.embedded.type.MessageType
import com.stark.shoot.application.port.`in`.chat.SendMessageUseCase
import com.stark.shoot.domain.chat.event.ChatEvent
import com.stark.shoot.domain.chat.event.EventType
import com.stark.shoot.domain.chat.message.ChatMessage
import com.stark.shoot.domain.chat.message.MessageContent
import com.stark.shoot.infrastructure.common.util.handleCompletion
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service

@Service
class SendMessageService(
    private val kafkaTemplate: KafkaTemplate<String, ChatEvent>
) : SendMessageUseCase {

    private val logger = KotlinLogging.logger {}

    override fun handleMessage(message: ChatMessageRequest, userId: String?) {
        // ChatMessage 생성
        val chatMessage = ChatMessage(
            roomId = message.roomId,
            senderId = userId ?: throw IllegalArgumentException("User ID is required"),
            content = MessageContent(
                text = message.content,
                type = MessageType.TEXT
            ),
            status = MessageStatus.SENT
        )

        // ChatEvent 생성
        val chatEvent = ChatEvent(
            type = EventType.MESSAGE_CREATED,
            data = chatMessage
        )

        // kafka로 이벤트 발행
        kafkaTemplate.send("chat-messages", message.roomId, chatEvent)
            .handleCompletion(
                onSuccess = { logger.info { "Message sent successfully: $chatMessage" } },
                onFailure = { logger.error(it) { "Failed to send message: $chatMessage" } }
            )
    }

}