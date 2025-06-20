package com.stark.shoot.domain.service.message

import com.stark.shoot.domain.chat.message.type.MessageStatus
import com.stark.shoot.domain.chat.message.ChatMessage
import com.stark.shoot.domain.chat.message.MessageContent
import com.stark.shoot.domain.chat.reaction.MessageReactions
import com.stark.shoot.domain.chat.room.ChatRoomId
import com.stark.shoot.domain.common.vo.UserId
import java.time.Instant

/**
 * 메시지 전달 관련 도메인 서비스
 * 메시지 전달(포워딩) 처리를 담당합니다.
 */
class MessageForwardDomainService {

    companion object {
        private const val FORWARDED_PREFIX = "[Forwarded] "
    }

    /**
     * 메시지를 전달(포워딩)하기 위한 새 메시지 내용을 생성합니다.
     *
     * @param originalMessage 원본 메시지
     * @return 전달용으로 수정된 메시지 내용
     */
    fun createForwardedContent(originalMessage: ChatMessage): MessageContent {
        // 메시지 내용을 복사 (전달임을 표시하기 위해 텍스트 앞에 접두사를 추가)
        return originalMessage.content.copy(
            text = "$FORWARDED_PREFIX${originalMessage.content.text}"
        )
    }

    /**
     * 전달할 메시지 객체를 생성합니다.
     *
     * @param targetRoomId 대상 채팅방 ID
     * @param forwardingUserId 전달하는 사용자 ID
     * @param forwardedContent 전달할 메시지 내용
     * @return 생성된 메시지 객체
     */
    fun createForwardedMessage(
        targetRoomId: ChatRoomId,
        forwardingUserId: UserId,
        forwardedContent: MessageContent
    ): ChatMessage {
        // 전달할 메시지 생성
        return ChatMessage(
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
    }

}