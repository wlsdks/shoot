package com.stark.shoot.adapter.`in`.web.message.reaction

import com.stark.shoot.adapter.`in`.web.dto.message.reaction.ReactionInfoDto
import com.stark.shoot.adapter.`in`.web.dto.message.reaction.ReactionListResponse
import com.stark.shoot.application.port.`in`.message.reaction.GetMessageReactionUseCase
import com.stark.shoot.domain.chat.message.vo.MessageId
import com.stark.shoot.domain.chat.reaction.type.ReactionType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*

@DisplayName("GetMessageReactionController 단위 테스트")
class GetMessageReactionControllerTest {

    private val getMessageReactionUseCase = mock(GetMessageReactionUseCase::class.java)
    private val controller = GetMessageReactionController(getMessageReactionUseCase)

    @Test
    @DisplayName("[happy] 메시지의 반응 목록을 조회한다")
    fun `메시지의 반응 목록을 조회한다`() {
        // given
        val messageId = "message123"
        
        val reactions = mapOf(
            "like" to setOf(1L, 2L, 3L),
            "sad" to setOf(4L, 5L),
            "curious" to setOf(6L)
        )
        
        `when`(getMessageReactionUseCase.getReactions(MessageId.from(messageId)))
            .thenReturn(reactions)

        // when
        val response = controller.getReactions(messageId)

        // then
        assertThat(response).isNotNull
        assertThat(response.success).isTrue()
        assertThat(response.data?.messageId).isEqualTo(messageId)
        assertThat(response.data?.reactions).hasSize(3)
        
        // 반응 타입별 검증
        val likeReaction = response.data?.reactions?.find { it.reactionType == "like" }
        assertThat(likeReaction).isNotNull
        assertThat(likeReaction?.emoji).isEqualTo("👍")
        assertThat(likeReaction?.description).isEqualTo("좋아요")
        assertThat(likeReaction?.userIds).containsExactlyInAnyOrder(1L, 2L, 3L)
        assertThat(likeReaction?.count).isEqualTo(3)
        
        val sadReaction = response.data?.reactions?.find { it.reactionType == "sad" }
        assertThat(sadReaction).isNotNull
        assertThat(sadReaction?.emoji).isEqualTo("😢")
        assertThat(sadReaction?.userIds).containsExactlyInAnyOrder(4L, 5L)
        assertThat(sadReaction?.count).isEqualTo(2)
        
        verify(getMessageReactionUseCase).getReactions(MessageId.from(messageId))
    }

    @Test
    @DisplayName("[happy] 메시지에 반응이 없는 경우 빈 목록을 반환한다")
    fun `메시지에 반응이 없는 경우 빈 목록을 반환한다`() {
        // given
        val messageId = "message123"
        
        `when`(getMessageReactionUseCase.getReactions(MessageId.from(messageId)))
            .thenReturn(emptyMap())

        // when
        val response = controller.getReactions(messageId)

        // then
        assertThat(response).isNotNull
        assertThat(response.success).isTrue()
        assertThat(response.data?.messageId).isEqualTo(messageId)
        assertThat(response.data?.reactions).isEmpty()
        
        verify(getMessageReactionUseCase).getReactions(MessageId.from(messageId))
    }

    @Test
    @DisplayName("[happy] 지원하는 반응 타입 목록을 조회한다")
    fun `지원하는 반응 타입 목록을 조회한다`() {
        // given
        val reactionTypes = listOf(
            ReactionType.LIKE,
            ReactionType.SAD,
            ReactionType.DISLIKE,
            ReactionType.ANGRY,
            ReactionType.CURIOUS,
            ReactionType.SURPRISED
        )
        
        `when`(getMessageReactionUseCase.getSupportedReactionTypes())
            .thenReturn(reactionTypes)

        // when
        val response = controller.getReactionTypes()

        // then
        assertThat(response).isNotNull
        assertThat(response.success).isTrue()
        assertThat(response.data).hasSize(6)
        
        // 각 반응 타입 검증
        val likeType = response.data?.find { it["code"] == "like" }
        assertThat(likeType).isNotNull
        assertThat(likeType?.get("emoji")).isEqualTo("👍")
        assertThat(likeType?.get("description")).isEqualTo("좋아요")
        
        val sadType = response.data?.find { it["code"] == "sad" }
        assertThat(sadType).isNotNull
        assertThat(sadType?.get("emoji")).isEqualTo("😢")
        assertThat(sadType?.get("description")).isEqualTo("슬퍼요")
        
        verify(getMessageReactionUseCase).getSupportedReactionTypes()
    }
}