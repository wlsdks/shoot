package com.stark.shoot.domain.chat.reaction

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("메시지 반응 테스트")
class MessageReactionsTest {

    @Nested
    @DisplayName("메시지 반응 생성 시")
    inner class CreateMessageReactions {

        @Test
        @DisplayName("빈 반응 맵으로 메시지 반응을 생성할 수 있다")
        fun `빈 반응 맵으로 메시지 반응을 생성할 수 있다`() {
            // when
            val reactions = MessageReactions()

            // then
            assertThat(reactions.reactions).isEmpty()
        }

        @Test
        @DisplayName("초기 반응 맵으로 메시지 반응을 생성할 수 있다")
        fun `초기 반응 맵으로 메시지 반응을 생성할 수 있다`() {
            // given
            val initialReactions = mapOf(
                "like" to setOf(1L, 2L),
                "heart" to setOf(3L)
            )

            // when
            val reactions = MessageReactions(initialReactions)

            // then
            assertThat(reactions.reactions).isEqualTo(initialReactions)
            assertThat(reactions.reactions).hasSize(2)
            assertThat(reactions.reactions["like"]).containsExactlyInAnyOrder(1L, 2L)
            assertThat(reactions.reactions["heart"]).containsExactly(3L)
        }
    }

    @Nested
    @DisplayName("반응 추가 시")
    inner class AddReaction {

        @Test
        @DisplayName("새로운 반응 타입을 추가할 수 있다")
        fun `새로운 반응 타입을 추가할 수 있다`() {
            // given
            val reactions = MessageReactions()
            val userId = 1L
            val reactionType = "like"

            // when
            val updatedReactions = reactions.addReaction(userId, reactionType)

            // then
            assertThat(updatedReactions.reactions).hasSize(1)
            assertThat(updatedReactions.reactions).containsKey(reactionType)
            assertThat(updatedReactions.reactions[reactionType]).containsExactly(userId)
        }

        @Test
        @DisplayName("기존 반응 타입에 새로운 사용자를 추가할 수 있다")
        fun `기존 반응 타입에 새로운 사용자를 추가할 수 있다`() {
            // given
            val initialReactions = mapOf(
                "like" to setOf(1L, 2L)
            )
            val reactions = MessageReactions(initialReactions)
            val userId = 3L
            val reactionType = "like"

            // when
            val updatedReactions = reactions.addReaction(userId, reactionType)

            // then
            assertThat(updatedReactions.reactions).hasSize(1)
            assertThat(updatedReactions.reactions).containsKey(reactionType)
            assertThat(updatedReactions.reactions[reactionType]).containsExactlyInAnyOrder(1L, 2L, 3L)
        }

        @Test
        @DisplayName("이미 추가된 사용자의 반응을 다시 추가해도 중복되지 않는다")
        fun `이미 추가된 사용자의 반응을 다시 추가해도 중복되지 않는다`() {
            // given
            val initialReactions = mapOf(
                "like" to setOf(1L, 2L)
            )
            val reactions = MessageReactions(initialReactions)
            val userId = 1L
            val reactionType = "like"

            // when
            val updatedReactions = reactions.addReaction(userId, reactionType)

            // then
            assertThat(updatedReactions.reactions).hasSize(1)
            assertThat(updatedReactions.reactions).containsKey(reactionType)
            assertThat(updatedReactions.reactions[reactionType]).containsExactlyInAnyOrder(1L, 2L)
        }
    }

    @Nested
    @DisplayName("반응 제거 시")
    inner class RemoveReaction {

        @Test
        @DisplayName("사용자의 반응을 제거할 수 있다")
        fun `사용자의 반응을 제거할 수 있다`() {
            // given
            val initialReactions = mapOf(
                "like" to setOf(1L, 2L, 3L)
            )
            val reactions = MessageReactions(initialReactions)
            val userId = 2L
            val reactionType = "like"

            // when
            val updatedReactions = reactions.removeReaction(userId, reactionType)

            // then
            assertThat(updatedReactions.reactions).hasSize(1)
            assertThat(updatedReactions.reactions).containsKey(reactionType)
            assertThat(updatedReactions.reactions[reactionType]).containsExactlyInAnyOrder(1L, 3L)
        }

        @Test
        @DisplayName("마지막 사용자의 반응을 제거하면 해당 반응 타입이 제거된다")
        fun `마지막 사용자의 반응을 제거하면 해당 반응 타입이 제거된다`() {
            // given
            val initialReactions = mapOf(
                "like" to setOf(1L, 2L),
                "heart" to setOf(3L)
            )
            val reactions = MessageReactions(initialReactions)
            val userId = 3L
            val reactionType = "heart"

            // when
            val updatedReactions = reactions.removeReaction(userId, reactionType)

            // then
            assertThat(updatedReactions.reactions).hasSize(1)
            assertThat(updatedReactions.reactions).containsKey("like")
            assertThat(updatedReactions.reactions).doesNotContainKey("heart")
        }

        @Test
        @DisplayName("존재하지 않는 반응 타입을 제거하려고 하면 변경 없이 그대로 반환한다")
        fun `존재하지 않는 반응 타입을 제거하려고 하면 변경 없이 그대로 반환한다`() {
            // given
            val initialReactions = mapOf(
                "like" to setOf(1L, 2L)
            )
            val reactions = MessageReactions(initialReactions)
            val userId = 3L
            val reactionType = "heart"

            // when
            val updatedReactions = reactions.removeReaction(userId, reactionType)

            // then
            assertThat(updatedReactions).isEqualTo(reactions)
            assertThat(updatedReactions.reactions).hasSize(1)
            assertThat(updatedReactions.reactions).containsKey("like")
            assertThat(updatedReactions.reactions["like"]).containsExactlyInAnyOrder(1L, 2L)
        }

        @Test
        @DisplayName("존재하지 않는 사용자의 반응을 제거하려고 해도 다른 사용자의 반응은 유지된다")
        fun `존재하지 않는 사용자의 반응을 제거하려고 해도 다른 사용자의 반응은 유지된다`() {
            // given
            val initialReactions = mapOf(
                "like" to setOf(1L, 2L)
            )
            val reactions = MessageReactions(initialReactions)
            val userId = 3L
            val reactionType = "like"

            // when
            val updatedReactions = reactions.removeReaction(userId, reactionType)

            // then
            assertThat(updatedReactions.reactions).hasSize(1)
            assertThat(updatedReactions.reactions).containsKey(reactionType)
            assertThat(updatedReactions.reactions[reactionType]).containsExactlyInAnyOrder(1L, 2L)
        }
    }

    @Nested
    @DisplayName("사용자 반응 타입 찾기 시")
    inner class FindUserExistingReactionType {

        @Test
        @DisplayName("사용자가 추가한 반응 타입을 찾을 수 있다")
        fun `사용자가 추가한 반응 타입을 찾을 수 있다`() {
            // given
            val initialReactions = mapOf(
                "like" to setOf(1L, 2L),
                "heart" to setOf(3L, 4L)
            )
            val reactions = MessageReactions(initialReactions)
            val userId = 3L

            // when
            val foundReactionType = reactions.findUserExistingReactionType(userId)

            // then
            assertThat(foundReactionType).isEqualTo("heart")
        }

        @Test
        @DisplayName("사용자가 반응을 추가하지 않았으면 null을 반환한다")
        fun `사용자가 반응을 추가하지 않았으면 null을 반환한다`() {
            // given
            val initialReactions = mapOf(
                "like" to setOf(1L, 2L),
                "heart" to setOf(3L, 4L)
            )
            val reactions = MessageReactions(initialReactions)
            val userId = 5L

            // when
            val foundReactionType = reactions.findUserExistingReactionType(userId)

            // then
            assertThat(foundReactionType).isNull()
        }

        @Test
        @DisplayName("반응이 없는 경우 null을 반환한다")
        fun `반응이 없는 경우 null을 반환한다`() {
            // given
            val reactions = MessageReactions()
            val userId = 1L

            // when
            val foundReactionType = reactions.findUserExistingReactionType(userId)

            // then
            assertThat(foundReactionType).isNull()
        }
    }
}