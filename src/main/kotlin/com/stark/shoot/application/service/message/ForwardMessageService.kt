package com.stark.shoot.application.service.message

import com.stark.shoot.adapter.out.persistence.mongodb.document.message.embedded.type.MessageStatus
import com.stark.shoot.application.port.`in`.message.ForwardMessageUseCase
import com.stark.shoot.application.port.out.message.LoadChatMessagePort
import com.stark.shoot.application.port.out.chatroom.LoadChatRoomPort
import com.stark.shoot.application.port.out.message.SaveChatMessagePort
import com.stark.shoot.application.port.out.chatroom.SaveChatRoomPort
import com.stark.shoot.domain.chat.message.ChatMessage
import com.stark.shoot.infrastructure.exception.web.ResourceNotFoundException
import com.stark.shoot.infrastructure.util.toObjectId
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class ForwardMessageService(
    private val loadChatMessagePort: LoadChatMessagePort,
    private val saveChatMessagePort: SaveChatMessagePort,
    private val loadChatRoomPort: LoadChatRoomPort,
    private val saveChatRoomPort: SaveChatRoomPort
) : ForwardMessageUseCase {

    /**
     * 메시지를 전달합니다. (메시지 복사 후 대상 채팅방에 저장)
     */
    override fun forwardMessage(
        originalMessageId: String,
        targetRoomId: String,
        forwardingUserId: String
    ): ChatMessage {
        // 1. 원본 메시지 조회
        val originalMessage = (loadChatMessagePort.findById(originalMessageId.toObjectId())
            ?: throw ResourceNotFoundException("메시지를 찾을 수 없습니다. messageId=$originalMessageId"))

        // 2. 메시지 내용을 복사 (전달임을 표시하기 위해 텍스트 앞에 "[Forwarded]"를 추가)
        val forwardedContent = originalMessage.content.copy(
            text = "[Forwarded] ${originalMessage.content.text}"
        )

        // 3. 새 전달 메시지 생성 (대상 채팅방, 전달한 사용자 정보로 생성)
        val newMessage = ChatMessage(
            roomId = targetRoomId,
            senderId = forwardingUserId,
            content = forwardedContent,
            status = MessageStatus.SAVED,
            replyToMessageId = null,
            reactions = emptyMap(),
            mentions = emptySet(),
            isDeleted = false,
            createdAt = Instant.now(),
            updatedAt = null
        )

        // 4. 대상 채팅방에 새 메시지 저장 (도메인 객체로 반환)
        val savedMessage = saveChatMessagePort.save(newMessage)

        // 5. 대상 채팅방 메타데이터 업데이트 (예: 마지막 메시지, 마지막 활동 시간 갱신)
        val chatRoom = loadChatRoomPort.findById(targetRoomId.toObjectId())
            ?: throw ResourceNotFoundException("대상 채팅방을 찾을 수 없습니다. id=$targetRoomId")

        // 마지막 메시지 및 마지막 활동 시간 갱신
        val updatedRoom = chatRoom.copy(
            lastMessageId = savedMessage.id,
            lastActiveAt = Instant.now()
        )

        // 저장
        saveChatRoomPort.save(updatedRoom)

        // 6. 전달된 메시지 반환
        return savedMessage
    }

}