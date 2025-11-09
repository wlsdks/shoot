package com.stark.shoot.application.port.out.message.reaction

import com.stark.shoot.domain.chat.message.vo.MessageId
import com.stark.shoot.domain.chat.reaction.MessageReaction
import com.stark.shoot.domain.chat.reaction.type.ReactionType
import com.stark.shoot.domain.chat.reaction.vo.MessageReactionId
import com.stark.shoot.domain.shared.UserId

/**
 * 메시지 리액션 조회 포트
 *
 * 리액션 조회를 담당합니다.
 */
interface MessageReactionQueryPort {

    /**
     * ID로 리액션 조회
     *
     * @param id 리액션 ID
     * @return 조회된 리액션 또는 null
     */
    fun findById(id: MessageReactionId): MessageReaction?

    /**
     * 메시지와 사용자로 리액션 조회
     *
     * @param messageId 메시지 ID
     * @param userId 사용자 ID
     * @return 조회된 리액션 또는 null
     */
    fun findByMessageIdAndUserId(messageId: MessageId, userId: UserId): MessageReaction?

    /**
     * 메시지의 모든 리액션 조회
     *
     * @param messageId 메시지 ID
     * @return 리액션 목록
     */
    fun findAllByMessageId(messageId: MessageId): List<MessageReaction>

    /**
     * 메시지의 특정 타입 리액션 조회
     *
     * @param messageId 메시지 ID
     * @param reactionType 리액션 타입
     * @return 리액션 목록
     */
    fun findAllByMessageIdAndReactionType(
        messageId: MessageId,
        reactionType: ReactionType
    ): List<MessageReaction>

    /**
     * 메시지의 리액션 개수 조회
     *
     * @param messageId 메시지 ID
     * @return 리액션 개수
     */
    fun countByMessageId(messageId: MessageId): Long

    /**
     * 메시지의 특정 타입 리액션 개수 조회
     *
     * @param messageId 메시지 ID
     * @param reactionType 리액션 타입
     * @return 리액션 개수
     */
    fun countByMessageIdAndReactionType(
        messageId: MessageId,
        reactionType: ReactionType
    ): Long

    /**
     * 메시지의 리액션 요약 조회
     * Map<ReactionType, Count>
     *
     * @param messageId 메시지 ID
     * @return 리액션 타입별 개수 맵
     */
    fun getReactionSummary(messageId: MessageId): Map<ReactionType, Long>
}
