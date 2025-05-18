package com.stark.shoot.application.service.message

import com.stark.shoot.adapter.out.persistence.mongodb.document.message.embedded.type.MessageStatus
import com.stark.shoot.application.port.`in`.message.ForwardMessageUseCase
import com.stark.shoot.application.port.out.chatroom.LoadChatRoomPort
import com.stark.shoot.application.port.out.chatroom.SaveChatRoomPort
import com.stark.shoot.application.port.out.message.LoadMessagePort
import com.stark.shoot.application.port.out.message.SaveMessagePort
import com.stark.shoot.domain.chat.message.ChatMessage
import com.stark.shoot.domain.chat.message.MessageContent
import com.stark.shoot.domain.chat.reaction.MessageReactions
import com.stark.shoot.infrastructure.annotation.UseCase
import com.stark.shoot.infrastructure.exception.web.ResourceNotFoundException
import com.stark.shoot.infrastructure.util.toObjectId
import java.time.Instant

@UseCase
class ForwardMessageService(
    private val loadMessagePort: LoadMessagePort,
    private val saveMessagePort: SaveMessagePort,
    private val loadChatRoomPort: LoadChatRoomPort,
    private val saveChatRoomPort: SaveChatRoomPort
) : ForwardMessageUseCase {

    companion object {
        private const val FORWARDED_PREFIX = "[Forwarded] "
    }

    /**
     * 메시지를 전달합니다. (메시지 복사 후 대상 채팅방에 저장)
     */
    override fun forwardMessage(
        originalMessageId: String,
        targetRoomId: Long,
        forwardingUserId: Long
    ): ChatMessage {
        // 1. 원본 메시지 조회
        val originalMessage = (loadMessagePort.findById(originalMessageId.toObjectId())
            ?: throw ResourceNotFoundException("메시지를 찾을 수 없습니다. messageId=$originalMessageId"))

        // 2. 메시지 내용을 복사 (전달임을 표시하기 위해 텍스트 앞에 접두사를 추가)
        val forwardedContent = originalMessage.content.copy(
            text = "$FORWARDED_PREFIX${originalMessage.content.text}"
        )

        // 3. 대상 채팅방에 새 메시지 저장 (도메인 객체로 반환)
        val savedForwardMessage = forwardMessageSave(targetRoomId, forwardingUserId, forwardedContent)

        // 4. 대상 채팅방 메타데이터 업데이트 (예: 마지막 메시지, 마지막 활동 시간 갱신)
        modifyChatRoomMetadata(targetRoomId, savedForwardMessage)

        // 5. 전달된 메시지 반환
        return savedForwardMessage
    }

    /**
     * 전달할 메시지 생성 및 저장
     *
     * @param targetRoomId 대상 채팅방 ID
     * @param forwardingUserId 전달하는 사용자 ID
     * @param forwardedContent 전달할 메시지 내용
     * @return 저장된 메시지
     */
    private fun forwardMessageSave(
        targetRoomId: Long,
        forwardingUserId: Long,
        forwardedContent: MessageContent
    ): ChatMessage {
        // 전달할 메시지 생성
        val newMessage = ChatMessage(
            roomId = targetRoomId,
            senderId = forwardingUserId,
            content = forwardedContent,
            status = MessageStatus.SAVED,
            replyToMessageId = null,
            messageReactions = MessageReactions(),
            mentions = emptySet(),
            isDeleted = false,
            createdAt = Instant.now(),
            updatedAt = null
        )

        // 대상 채팅방에 새 메시지 저장 (도메인 객체로 반환)
        val savedMessage = saveMessagePort.save(newMessage)

        return savedMessage
    }

    /**
     * 대상 채팅방 메타데이터 업데이트
     *
     * @param targetRoomId 대상 채팅방 ID
     * @param savedForwardMessage 전달된 메시지
     */
    private fun modifyChatRoomMetadata(
        targetRoomId: Long,
        savedForwardMessage: ChatMessage
    ) {
        // 대상 채팅방 조회
        val chatRoom = loadChatRoomPort.findById(targetRoomId)
            ?: throw ResourceNotFoundException("대상 채팅방을 찾을 수 없습니다. id=$targetRoomId")

        // 마지막 메시지 및 마지막 활동 시간 갱신
        val updatedRoom = chatRoom.copy(
            lastMessageId = savedForwardMessage.id,
            lastActiveAt = Instant.now()
        )

        // 저장
        saveChatRoomPort.save(updatedRoom)
    }

}
