package com.stark.shoot.application.service.message.forward

import com.stark.shoot.application.port.`in`.message.ForwardMessageUseCase
import com.stark.shoot.application.port.`in`.message.command.ForwardMessageCommand
import com.stark.shoot.application.port.out.chatroom.ChatRoomCommandPort
import com.stark.shoot.application.port.out.chatroom.ChatRoomQueryPort
import com.stark.shoot.application.port.out.message.MessageCommandPort
import com.stark.shoot.application.port.out.message.MessageQueryPort
import com.stark.shoot.domain.chat.message.ChatMessage
import com.stark.shoot.domain.chat.message.service.MessageForwardDomainService
import com.stark.shoot.domain.chatroom.service.ChatRoomMetadataDomainService
import com.stark.shoot.domain.chatroom.vo.ChatRoomId
import com.stark.shoot.infrastructure.annotation.UseCase
import com.stark.shoot.domain.chatroom.exception.ChatRoomException
import com.stark.shoot.infrastructure.exception.web.ResourceNotFoundException

@UseCase
class ForwardMessageService(
    private val messageQueryPort: MessageQueryPort,
    private val messageCommandPort: MessageCommandPort,
    private val chatRoomQueryPort: ChatRoomQueryPort,
    private val chatRoomCommandPort: ChatRoomCommandPort,
    private val messageForwardDomainService: MessageForwardDomainService,
    private val chatRoomMetadataDomainService: ChatRoomMetadataDomainService
) : ForwardMessageUseCase {

    /**
     * 메시지를 전달합니다. (메시지 복사 후 대상 채팅방에 저장)
     *
     * 권한 검증:
     * - 사용자가 원본 채팅방의 참여자인지 확인
     * - 사용자가 대상 채팅방의 참여자인지 확인
     *
     * 첨부파일 처리:
     * - 첨부파일 URL은 원본 메시지와 공유됨 (참조 방식)
     * - 원본 메시지가 삭제되어도 첨부파일은 유지됨 (파일 시스템 레벨 참조)
     *
     * @param command 메시지 전달 커맨드 (원본 메시지 ID, 대상 채팅방 ID, 전달하는 사용자 ID)
     * @return 전달된 메시지
     * @throws ResourceNotFoundException 메시지나 채팅방을 찾을 수 없는 경우
     * @throws ChatRoomException.NotParticipant 사용자가 채팅방 참여자가 아닌 경우
     */
    override fun forwardMessage(command: ForwardMessageCommand): ChatMessage {
        // 1. 원본 메시지 조회
        val originalMessage = (messageQueryPort.findById(command.originalMessageId))
            ?: throw ResourceNotFoundException("메시지를 찾을 수 없습니다. messageId=${command.originalMessageId}")

        // 2. 원본 채팅방 조회 및 권한 검증
        val sourceChatRoom = chatRoomQueryPort.findById(originalMessage.roomId)
            ?: throw ResourceNotFoundException("원본 채팅방을 찾을 수 없습니다. id=${originalMessage.roomId}")

        if (command.forwardingUserId !in sourceChatRoom.participants) {
            throw ChatRoomException.NotParticipant("원본 채팅방의 참여자가 아닙니다. roomId=${originalMessage.roomId}")
        }

        // 3. 대상 채팅방 조회 및 권한 검증
        val targetChatRoom = chatRoomQueryPort.findById(command.targetRoomId)
            ?: throw ResourceNotFoundException("대상 채팅방을 찾을 수 없습니다. id=${command.targetRoomId}")

        if (command.forwardingUserId !in targetChatRoom.participants) {
            throw ChatRoomException.NotParticipant("대상 채팅방의 참여자가 아닙니다. roomId=${command.targetRoomId}")
        }

        // 4. 도메인 서비스를 사용하여 전달할 메시지 내용 생성
        // Note: 첨부파일 URL은 원본과 공유됨 (파일 시스템 레벨에서 참조 카운팅 필요 시 별도 구현)
        val forwardedContent = messageForwardDomainService.createForwardedContent(originalMessage)

        // 5. 도메인 서비스를 사용하여 전달할 메시지 객체 생성
        val forwardedMessage = messageForwardDomainService.createForwardedMessage(
            targetRoomId = command.targetRoomId,
            forwardingUserId = command.forwardingUserId,
            forwardedContent = forwardedContent
        )

        // 6. 메시지 저장
        val savedForwardMessage = messageCommandPort.save(forwardedMessage)

        // 7. 대상 채팅방 메타데이터 업데이트
        updateChatRoomMetadata(command.targetRoomId, savedForwardMessage)

        // 8. 전달된 메시지 반환
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
        val chatRoom = chatRoomQueryPort.findById(targetRoomId)
            ?: throw ResourceNotFoundException("대상 채팅방을 찾을 수 없습니다. id=$targetRoomId")

        // 도메인 서비스를 사용하여 채팅방 메타데이터 업데이트 (DDD 개선)
        val messageId = savedForwardMessage.id?.value
            ?: throw IllegalStateException("Saved message has no ID")
        val updatedRoom = chatRoomMetadataDomainService.updateChatRoomWithNewMessage(
            chatRoom = chatRoom,
            messageId = messageId,
            createdAt = savedForwardMessage.createdAt ?: java.time.Instant.now()
        )

        // 저장
        chatRoomCommandPort.save(updatedRoom)
    }

}
