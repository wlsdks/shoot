package com.stark.shoot.application.service.message

import com.stark.shoot.adapter.`in`.web.dto.message.ChatMessageRequest
import com.stark.shoot.adapter.out.persistence.mongodb.document.message.embedded.type.MessageStatus
import com.stark.shoot.adapter.out.persistence.mongodb.document.message.embedded.type.MessageType
import com.stark.shoot.application.port.`in`.message.SendMessageUseCase
import com.stark.shoot.application.port.out.kafka.KafkaMessagePublishPort
import com.stark.shoot.domain.chat.event.ChatEvent
import com.stark.shoot.domain.chat.event.EventType
import com.stark.shoot.domain.chat.message.ChatMessage
import com.stark.shoot.domain.chat.message.MessageContent
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.concurrent.CompletableFuture

@Service
class SendMessageService(
    private val kafkaMessagePublishPort: KafkaMessagePublishPort
) : SendMessageUseCase {

    override fun handleMessage(
        message: ChatMessageRequest
    ): CompletableFuture<String?> {
        // 임시 ID 복사
        val tempId = message.tempId

        // ChatMessage 생성
        val chatMessage = ChatMessage(
            roomId = message.roomId,
            senderId = message.senderId ?: throw IllegalArgumentException("User ID is required"),
            content = MessageContent(
                text = message.content.text,
                type = MessageType.TEXT
            ),
            status = MessageStatus.SAVED,
            createdAt = Instant.now()
        )

        // 메타데이터 복사 (tempId와 status 포함)
        if (message.metadata.isNotEmpty()) {
            chatMessage.metadata = message.metadata
        }

        // ChatEvent 생성
        val chatEvent = ChatEvent(
            type = EventType.MESSAGE_CREATED,
            data = chatMessage
        )

        // Kafka로 이벤트 발행
        val future = kafkaMessagePublishPort.publishChatEvent(
            topic = "chat-messages",
            key = message.roomId,
            event = chatEvent
        )

        // Kafka 발행 성공 후 tempId 반환 (실제 저장 ID는 Kafka Consumer에서 생성됨)
        return future.thenApply {
            tempId
        }
    }

}