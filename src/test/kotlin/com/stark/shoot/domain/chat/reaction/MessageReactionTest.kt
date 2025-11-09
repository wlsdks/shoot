package com.stark.shoot.domain.chat.reaction

import com.stark.shoot.domain.chat.message.vo.MessageId
import com.stark.shoot.domain.chat.reaction.type.ReactionType
import com.stark.shoot.domain.shared.UserId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("MessageReaction Aggregate 테스트")
class MessageReactionTest {

    @Test
    @DisplayName("리액션 생성 - 팩토리 메서드")
    fun `create reaction using factory method`() {
        // Given
        val messageId = MessageId.from("msg-1")
        val userId = UserId.from(100L)
        val reactionType = ReactionType.LIKE

        // When
        val reaction = MessageReaction.create(
            messageId = messageId,
            userId = userId,
            reactionType = reactionType
        )

        // Then
        assertThat(reaction.messageId).isEqualTo(messageId)
        assertThat(reaction.userId).isEqualTo(userId)
        assertThat(reaction.reactionType).isEqualTo(reactionType)
        assertThat(reaction.id).isNull()
        assertThat(reaction.createdAt).isNotNull()
        assertThat(reaction.updatedAt).isNull()
    }

    @Test
    @DisplayName("리액션 타입 변경")
    fun `change reaction type`() {
        // Given
        val reaction = MessageReaction.create(
            messageId = MessageId.from("msg-1"),
            userId = UserId.from(100L),
            reactionType = ReactionType.LIKE
        )

        // When
        reaction.changeReactionType(ReactionType.SAD)

        // Then
        assertThat(reaction.reactionType).isEqualTo(ReactionType.SAD)
        assertThat(reaction.updatedAt).isNotNull()
    }

    @Test
    @DisplayName("동일한 리액션 타입으로 변경 시 updatedAt 변경 안됨")
    fun `change to same reaction type should not update updatedAt`() {
        // Given
        val reaction = MessageReaction.create(
            messageId = MessageId.from("msg-1"),
            userId = UserId.from(100L),
            reactionType = ReactionType.LIKE
        )
        val originalUpdatedAt = reaction.updatedAt

        // When
        reaction.changeReactionType(ReactionType.LIKE)

        // Then
        assertThat(reaction.reactionType).isEqualTo(ReactionType.LIKE)
        assertThat(reaction.updatedAt).isEqualTo(originalUpdatedAt)
    }

    @Test
    @DisplayName("특정 메시지에 속하는지 확인")
    fun `check if reaction belongs to message`() {
        // Given
        val messageId = MessageId.from("msg-1")
        val reaction = MessageReaction.create(
            messageId = messageId,
            userId = UserId.from(100L),
            reactionType = ReactionType.LIKE
        )

        // When & Then
        assertThat(reaction.belongsToMessage(messageId)).isTrue()
        assertThat(reaction.belongsToMessage(MessageId.from("msg-2"))).isFalse()
    }

    @Test
    @DisplayName("특정 사용자의 리액션인지 확인")
    fun `check if reaction belongs to user`() {
        // Given
        val userId = UserId.from(100L)
        val reaction = MessageReaction.create(
            messageId = MessageId.from("msg-1"),
            userId = userId,
            reactionType = ReactionType.LIKE
        )

        // When & Then
        assertThat(reaction.belongsToUser(userId)).isTrue()
        assertThat(reaction.belongsToUser(UserId.from(200L))).isFalse()
    }

    @Test
    @DisplayName("리액션 타입별 생성 테스트")
    fun `create reactions with different types`() {
        // Given
        val messageId = MessageId.from("msg-1")
        val userId = UserId.from(100L)

        // When
        val likeReaction = MessageReaction.create(messageId, userId, ReactionType.LIKE)
        val sadReaction = MessageReaction.create(messageId, userId, ReactionType.SAD)
        val angryReaction = MessageReaction.create(messageId, userId, ReactionType.ANGRY)

        // Then
        assertThat(likeReaction.reactionType).isEqualTo(ReactionType.LIKE)
        assertThat(sadReaction.reactionType).isEqualTo(ReactionType.SAD)
        assertThat(angryReaction.reactionType).isEqualTo(ReactionType.ANGRY)
    }

    @Test
    @DisplayName("리액션 타입 여러 번 변경")
    fun `change reaction type multiple times`() {
        // Given
        val reaction = MessageReaction.create(
            messageId = MessageId.from("msg-1"),
            userId = UserId.from(100L),
            reactionType = ReactionType.LIKE
        )

        // When
        reaction.changeReactionType(ReactionType.SAD)
        reaction.changeReactionType(ReactionType.ANGRY)
        reaction.changeReactionType(ReactionType.SURPRISED)

        // Then
        assertThat(reaction.reactionType).isEqualTo(ReactionType.SURPRISED)
        assertThat(reaction.updatedAt).isNotNull()
    }
}
