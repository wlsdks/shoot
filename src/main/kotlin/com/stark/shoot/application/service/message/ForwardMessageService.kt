package com.stark.shoot.application.service.message

import com.stark.shoot.application.port.`in`.message.ForwardMessageUseCase
import com.stark.shoot.application.port.out.chatroom.LoadChatRoomPort
import com.stark.shoot.application.port.out.chatroom.SaveChatRoomPort
import com.stark.shoot.application.port.out.message.LoadMessagePort
import com.stark.shoot.application.port.out.message.SaveMessagePort
import com.stark.shoot.domain.chat.message.ChatMessage
import com.stark.shoot.domain.chat.room.ChatRoomId
import com.stark.shoot.domain.common.vo.MessageId
import com.stark.shoot.domain.common.vo.UserId
import com.stark.shoot.domain.service.chatroom.ChatRoomMetadataDomainService
import com.stark.shoot.domain.service.message.MessageForwardDomainService
import com.stark.shoot.infrastructure.annotation.UseCase
import com.stark.shoot.infrastructure.exception.web.ResourceNotFoundException

@UseCase
class ForwardMessageService(
    private val loadMessagePort: LoadMessagePort,
    private val saveMessagePort: SaveMessagePort,
    private val loadChatRoomPort: LoadChatRoomPort,
    private val saveChatRoomPort: SaveChatRoomPort,
    private val messageForwardDomainService: MessageForwardDomainService,
    private val chatRoomMetadataDomainService: ChatRoomMetadataDomainService
) : ForwardMessageUseCase {

    /**
     * 메시지를 전달합니다. (메시지 복사 후 대상 채팅방에 저장)
     */
    override fun forwardMessage(
        originalMessageId: MessageId,
        targetRoomId: ChatRoomId,
        forwardingUserId: UserId
    ): ChatMessage {
        // 1. 원본 메시지 조회
        val originalMessage = (loadMessagePort.findById(originalMessageId))
            ?: throw ResourceNotFoundException("메시지를 찾을 수 없습니다. messageId=$originalMessageId")

        // 2. 도메인 서비스를 사용하여 전달할 메시지 내용 생성
        val forwardedContent = messageForwardDomainService.createForwardedContent(originalMessage)

        // 3. 도메인 서비스를 사용하여 전달할 메시지 객체 생성
        val forwardedMessage = messageForwardDomainService.createForwardedMessage(
            targetRoomId = targetRoomId,
            forwardingUserId = forwardingUserId,
            forwardedContent = forwardedContent
        )

        // 4. 메시지 저장
        val savedForwardMessage = saveMessagePort.save(forwardedMessage)

        // 5. 대상 채팅방 메타데이터 업데이트
        updateChatRoomMetadata(targetRoomId, savedForwardMessage)

        // 6. 전달된 메시지 반환
        return savedForwardMessage
    }

    /**
     * 대상 채팅방 메타데이터 업데이트
     *
     * @param targetRoomId 대상 채팅방 ID
     * @param savedForwardMessage 전달된 메시지
     */
    private fun updateChatRoomMetadata(
        targetRoomId: ChatRoomId,
        savedForwardMessage: ChatMessage
    ) {
        // 대상 채팅방 조회
        val chatRoom = loadChatRoomPort.findById(targetRoomId)
            ?: throw ResourceNotFoundException("대상 채팅방을 찾을 수 없습니다. id=$targetRoomId")

        // 도메인 서비스를 사용하여 채팅방 메타데이터 업데이트
        val updatedRoom = chatRoomMetadataDomainService.updateChatRoomWithNewMessage(
            chatRoom = chatRoom,
            message = savedForwardMessage
        )

        // 저장
        saveChatRoomPort.save(updatedRoom)
    }

}
