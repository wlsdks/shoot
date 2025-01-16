package com.stark.shoot.application.service

import com.stark.shoot.application.port.`in`.SendMessageUseCase
import com.stark.shoot.application.port.out.EventPublisher
import com.stark.shoot.application.port.out.LoadChatRoomPort
import com.stark.shoot.application.port.out.SaveChatMessagePort
import com.stark.shoot.domain.chat.event.ChatMessageSentEvent
import com.stark.shoot.domain.chat.message.ChatMessage
import com.stark.shoot.infrastructure.common.exception.ResourceNotFoundException
import com.stark.shoot.infrastructure.common.toObjectId
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ChatService(
    private val saveChatMessagePort: SaveChatMessagePort,
    private val loadChatRoomPort: LoadChatRoomPort,
    private val eventPublisher: EventPublisher
) : SendMessageUseCase {

    @Transactional
    override fun sendMessage(
        roomId: String,
        senderId: String,
        messageContent: ChatMessage
    ): ChatMessage {
        // 채팅방 유효성 확인
        val chatRoom = loadChatRoomPort.findById(roomId.toObjectId())
            ?: throw ResourceNotFoundException("채팅방을 찾을 수 없습니다. roomId=$roomId")

        // 메시지 생성 및 저장
        val chatMessage = ChatMessage(
            roomId = roomId,
            senderId = senderId,
            content = messageContent.content,
            status = messageContent.status
        )
        val savedMessage = saveChatMessagePort.save(chatMessage)

        // 이벤트 발행
        eventPublisher.publish(ChatMessageSentEvent(savedMessage))

        return savedMessage
    }

}