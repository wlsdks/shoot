package com.stark.shoot.domain.chat.message.service

import com.stark.shoot.domain.chat.message.ChatMessage
import com.stark.shoot.domain.event.MessagePinEvent
import com.stark.shoot.domain.shared.UserId

/**
 * 메시지 고정 관련 도메인 서비스
 * 메시지 고정/해제 시 필요한 도메인 이벤트를 생성합니다.
 */
class MessagePinDomainService {

    /**
     * 메시지 고정 상태 변경에 따른 도메인 이벤트를 생성합니다.
     *
     * @param message 메시지
     * @param userId 사용자 ID
     * @param isPinned 고정 여부
     * @return 생성된 도메인 이벤트
     */
    fun createPinEvent(
        message: ChatMessage,
        userId: UserId,
        isPinned: Boolean
    ): MessagePinEvent? {
        val messageId = message.id ?: return null

        return MessagePinEvent.create(
            messageId = messageId,
            roomId = message.roomId,
            isPinned = isPinned,
            userId = userId
        )
    }

}
