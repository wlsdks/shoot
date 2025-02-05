package com.stark.shoot.application.service.chat

import com.stark.shoot.adapter.`in`.web.dto.message.ChatMessageRequest
import com.stark.shoot.adapter.out.persistence.mongodb.document.message.embedded.type.MessageStatus
import com.stark.shoot.adapter.out.persistence.mongodb.document.message.embedded.type.MessageType
import com.stark.shoot.application.port.`in`.chat.SendMessageUseCase
import com.stark.shoot.application.port.out.kafka.KafkaMessagePublishPort
import com.stark.shoot.domain.chat.event.ChatEvent
import com.stark.shoot.domain.chat.event.EventType
import com.stark.shoot.domain.chat.message.ChatMessage
import com.stark.shoot.domain.chat.message.MessageContent
import org.springframework.stereotype.Service
import java.util.concurrent.CompletableFuture

@Service
class SendMessageService(
    private val kafkaMessagePublishPort: KafkaMessagePublishPort
) : SendMessageUseCase {

    override fun handleMessage(
        message: ChatMessageRequest
    ): CompletableFuture<Void> {
        // ChatMessage 생성
        val chatMessage = ChatMessage(
            roomId = message.roomId,
            senderId = message.senderId ?: throw IllegalArgumentException("User ID is required"),
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

        // kafka로 이벤트 발행하고 CompletableFuture 반환
        return kafkaMessagePublishPort.publishChatEvent(
            topic = "chat-messages",
            key = message.roomId,
            event = chatEvent
        )
    }

}