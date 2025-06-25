package com.stark.shoot.domain.chat.reaction

import com.stark.shoot.domain.chat.reaction.type.ReactionType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("리액션 타입 비즈니스 로직 테스트")
class ReactionTypeTest {
    @Test
    @DisplayName("[happy] code 값으로 리액션 타입을 조회할 수 있다")
    fun `code 값으로 리액션 타입을 조회할 수 있다`() {
        val result = ReactionType.fromCode("like")
        assertThat(result).isEqualTo(ReactionType.LIKE)
    }

    @Test
    @DisplayName("[happy] 없는 코드면 null을 반환한다")
    fun `없는 코드면 null을 반환한다`() {
        val result = ReactionType.fromCode("unknown")
        assertThat(result).isNull()
    }

    @Test
    @DisplayName("[happy] 이모지 값으로 리액션 타입을 조회할 수 있다")
    fun `이모지 값으로 리액션 타입을 조회할 수 있다`() {
        val result = ReactionType.fromEmoji("😡")
        assertThat(result).isEqualTo(ReactionType.ANGRY)
    }

    @Test
    @DisplayName("[happy] 없는 이모지면 null을 반환한다")
    fun `없는 이모지면 null을 반환한다`() {
        val result = ReactionType.fromEmoji("🤷")
        assertThat(result).isNull()
    }

    @Test
    @DisplayName("[happy] 지원하는 모든 리액션 타입 정보를 가져올 수 있다")
    fun `지원하는 모든 리액션 타입 정보를 가져올 수 있다`() {
        val list = ReactionType.getAllReactionTypes()
        assertThat(list).hasSize(ReactionType.entries.size)
        assertThat(list[0]).containsKeys("code", "emoji", "description")
    }
}
