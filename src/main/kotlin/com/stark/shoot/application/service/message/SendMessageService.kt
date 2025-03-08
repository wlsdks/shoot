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
import com.stark.shoot.infrastructure.annotation.UseCase
import java.time.Instant
import java.util.concurrent.CompletableFuture

@UseCase
class SendMessageService(
    private val kafkaMessagePublishPort: KafkaMessagePublishPort
) : SendMessageUseCase {

    /**
     * 메시지 전송 처리 (Kafka로 이벤트 발행)
     *
     * @param requestMessage ChatMessageRequest
     * @return CompletableFuture<String?>
     */
    override fun handleMessage(
        requestMessage: ChatMessageRequest
    ): CompletableFuture<String?> {
        // 임시 ID 복사
        val tempId = requestMessage.tempId

        // ChatMessage 생성
        val chatMessage = createChatMessage(requestMessage)

        // ChatEvent 생성
        val chatEvent = createChatEvent(requestMessage, chatMessage)

        // Kafka로 이벤트 발행
        return publishKafkaMessage(requestMessage, chatEvent, tempId)
    }

    /**
     * ChatMessage 객체 생성
     *
     * @param message ChatMessageRequest
     * @return ChatMessage
     */
    private fun createChatMessage(
        message: ChatMessageRequest
    ): ChatMessage {
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

        return chatMessage
    }

    /**
     * ChatEvent 객체 생성
     * kafka에 전송할 이벤트 객체를 생성합니다.
     *
     * @param requestMessage ChatMessageRequest
     * @param chatMessage ChatMessage
     * @return ChatEvent
     */
    private fun createChatEvent(
        requestMessage: ChatMessageRequest,
        chatMessage: ChatMessage
    ): ChatEvent {
        // 메타데이터 복사 (tempId와 status 포함)
        if (requestMessage.metadata.isNotEmpty()) {
            chatMessage.metadata = requestMessage.metadata
        }

        // ChatEvent 생성
        val chatEvent = ChatEvent(
            type = EventType.MESSAGE_CREATED,
            data = chatMessage
        )

        return chatEvent
    }

    /**
     * Kafka로 이벤트 발행
     *
     * @param requestMessage ChatMessageRequest
     * @param chatEvent ChatEvent
     * @param tempId String?
     * @return CompletableFuture<String?>
     */
    private fun publishKafkaMessage(
        requestMessage: ChatMessageRequest,
        chatEvent: ChatEvent,
        tempId: String?
    ): CompletableFuture<String?> {
        // Kafka로 이벤트 발행
        val future = kafkaMessagePublishPort.publishChatEvent(
            topic = "chat-messages",
            key = requestMessage.roomId,
            event = chatEvent
        )

        // Kafka 발행 성공 후 tempId 반환 (실제 저장 ID는 Kafka Consumer에서 생성됨)
        return future.thenApply {
            tempId
        }
    }

}