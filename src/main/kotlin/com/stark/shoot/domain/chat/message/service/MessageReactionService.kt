package com.stark.shoot.domain.chat.message.service

import com.stark.shoot.domain.chat.message.vo.ReactionToggleResult
import com.stark.shoot.domain.event.MessageReactionEvent
import com.stark.shoot.domain.exception.MessageException

/**
 * 메시지 리액션 관련 도메인 서비스
 * 메시지 리액션 토글 결과를 처리하고 필요한 도메인 이벤트를 생성합니다.
 */
class MessageReactionService {

    /**
     * 리액션 토글 결과를 처리하고 필요한 도메인 이벤트를 생성합니다.
     *
     * @param result 리액션 토글 결과
     * @return 생성된 도메인 이벤트 목록
     */
    fun processReactionToggleResult(result: ReactionToggleResult): List<MessageReactionEvent> {
        val events = mutableListOf<MessageReactionEvent>()
        val message = result.message
        val userId = result.userId
        val messageId = message.id ?: throw MessageException.MissingId()

        // 리액션 교체인 경우 (기존 리액션 제거 후 새 리액션 추가)
        if (result.isReplacement && result.previousReactionType != null) {
            // 기존 리액션 제거 이벤트
            events.add(
                MessageReactionEvent.create(
                    messageId = messageId,
                    roomId = message.roomId,
                    userId = userId,
                    reactionType = result.previousReactionType,
                    isAdded = false,
                    isReplacement = true
                )
            )

            // 새 리액션 추가 이벤트
            events.add(
                MessageReactionEvent.create(
                    messageId = messageId,
                    roomId = message.roomId,
                    userId = userId,
                    reactionType = result.reactionType,
                    isAdded = true,
                    isReplacement = true
                )
            )
        } else {
            // 일반 추가/제거 이벤트
            events.add(
                MessageReactionEvent.create(
                    messageId = messageId,
                    roomId = message.roomId,
                    userId = userId,
                    reactionType = result.reactionType,
                    isAdded = result.isAdded,
                    isReplacement = false
                )
            )
        }

        return events
    }
}
