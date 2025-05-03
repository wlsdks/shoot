package com.stark.shoot.application.port.`in`.message.reaction

import com.stark.shoot.adapter.`in`.web.dto.message.reaction.ReactionResponse
import com.stark.shoot.infrastructure.enumerate.ReactionType

interface MessageReactionUseCase {

    /**
     * 메시지에 리액션을 토글합니다.
     * 같은 리액션을 선택하면 제거하고, 다른 리액션을 선택하면 기존 리액션을 제거하고 새 리액션을 추가합니다.
     *
     * @param messageId 메시지 ID
     * @param userId 사용자 ID
     * @param reactionType 리액션 타입
     * @return 업데이트된 메시지의 리액션 정보
     */
    fun toggleReaction(
        messageId: String,
        userId: Long,
        reactionType: String
    ): ReactionResponse

    fun getReactions(messageId: String): Map<String, Set<Long>>
    fun getSupportedReactionTypes(): List<ReactionType>
}
