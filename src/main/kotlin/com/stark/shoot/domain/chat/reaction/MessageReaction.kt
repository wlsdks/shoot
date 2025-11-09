package com.stark.shoot.domain.chat.reaction

import com.stark.shoot.domain.chat.message.vo.MessageId
import com.stark.shoot.domain.chat.reaction.type.ReactionType
import com.stark.shoot.domain.chat.reaction.vo.MessageReactionId
import com.stark.shoot.domain.shared.UserId
import com.stark.shoot.infrastructure.annotation.AggregateRoot
import java.time.Instant

/**
 * 메시지 리액션 Aggregate
 *
 * DDD Aggregate 원칙:
 * - 메시지와 독립적인 생명주기 (별도 트랜잭션 경계)
 * - ID 참조로 메시지와 연결
 * - 높은 동시성 처리 (여러 사용자가 동시에 리액션 추가)
 *
 * 비즈니스 규칙:
 * - 하나의 메시지에 사용자당 1개의 리액션만 가능
 * - 다른 리액션 선택 시 기존 리액션은 자동 제거
 */
@AggregateRoot
data class MessageReaction(
    val id: MessageReactionId? = null,
    val messageId: MessageId,
    val userId: UserId,
    var reactionType: ReactionType,
    val createdAt: Instant = Instant.now(),
    var updatedAt: Instant? = null
) {
    companion object {
        /**
         * 새 리액션 생성
         *
         * @param messageId 메시지 ID
         * @param userId 사용자 ID
         * @param reactionType 리액션 타입
         * @return 생성된 MessageReaction
         */
        fun create(
            messageId: MessageId,
            userId: UserId,
            reactionType: ReactionType
        ): MessageReaction {
            return MessageReaction(
                messageId = messageId,
                userId = userId,
                reactionType = reactionType
            )
        }
    }

    /**
     * 리액션 타입 변경
     *
     * @param newType 새 리액션 타입
     */
    fun changeReactionType(newType: ReactionType) {
        if (reactionType != newType) {
            reactionType = newType
            updatedAt = Instant.now()
        }
    }

    /**
     * 특정 메시지에 대한 리액션인지 확인
     *
     * @param messageId 메시지 ID
     * @return 해당 메시지의 리액션이면 true
     */
    fun belongsToMessage(messageId: MessageId): Boolean {
        return this.messageId == messageId
    }

    /**
     * 특정 사용자의 리액션인지 확인
     *
     * @param userId 사용자 ID
     * @return 해당 사용자의 리액션이면 true
     */
    fun belongsToUser(userId: UserId): Boolean {
        return this.userId == userId
    }
}
