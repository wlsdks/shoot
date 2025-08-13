package com.stark.shoot.adapter.`in`.rest.message.reaction

import com.stark.shoot.adapter.`in`.rest.dto.message.reaction.ReactionRequest
import com.stark.shoot.adapter.`in`.rest.dto.message.reaction.ReactionResponse
import com.stark.shoot.application.port.`in`.message.reaction.ToggleMessageReactionUseCase
import com.stark.shoot.application.port.`in`.message.reaction.command.ToggleMessageReactionCommand
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import org.springframework.security.core.Authentication
import java.time.Instant

@DisplayName("ToggleMessageReactionController 단위 테스트")
class ToggleMessageReactionControllerTest {

    private val toggleMessageReactionUseCase = mock(ToggleMessageReactionUseCase::class.java)
    private val authentication = mock(Authentication::class.java)
    private val controller = ToggleMessageReactionController(toggleMessageReactionUseCase)

    @Test
    @DisplayName("[happy] 메시지 반응 토글 요청을 처리하고 업데이트된 반응 정보를 반환한다")
    fun `메시지 반응 토글 요청을 처리하고 업데이트된 반응 정보를 반환한다`() {
        // given
        val messageId = "message123"
        val userId = 1L
        val reactionType = "like"
        
        val request = ReactionRequest(messageId, reactionType, userId)
        
        `when`(authentication.name).thenReturn(userId.toString())
        
        val updatedReactions = mapOf(
            "like" to setOf(1L, 2L, 3L),
            "sad" to setOf(4L, 5L)
        )
        
        val updatedAt = Instant.now().toString()
        val response = ReactionResponse.from(messageId, updatedReactions, updatedAt)
        
        val command = ToggleMessageReactionCommand.of(messageId, authentication, reactionType)
        `when`(toggleMessageReactionUseCase.toggleReaction(command)).thenReturn(response)

        // when
        val result = controller.toggleReaction(messageId, request, authentication)

        // then
        assertThat(result).isNotNull
        assertThat(result.success).isTrue()
        assertThat(result.data).isEqualTo(response)
        assertThat(result.message).isEqualTo("반응이 토글되었습니다.")
        
        verify(toggleMessageReactionUseCase).toggleReaction(command)
    }

    @Test
    @DisplayName("[happy] 메시지 반응 제거 요청을 처리하고 업데이트된 반응 정보를 반환한다")
    fun `메시지 반응 제거 요청을 처리하고 업데이트된 반응 정보를 반환한다`() {
        // given
        val messageId = "message123"
        val userId = 1L
        val reactionType = "sad"
        
        val request = ReactionRequest(messageId, reactionType, userId)
        
        `when`(authentication.name).thenReturn(userId.toString())
        
        // 반응이 제거된 상태의 응답
        val updatedReactions = mapOf(
            "like" to setOf(2L, 3L)
        )
        
        val updatedAt = Instant.now().toString()
        val response = ReactionResponse.from(messageId, updatedReactions, updatedAt)
        
        val command = ToggleMessageReactionCommand.of(messageId, authentication, reactionType)
        `when`(toggleMessageReactionUseCase.toggleReaction(command)).thenReturn(response)

        // when
        val result = controller.toggleReaction(messageId, request, authentication)

        // then
        assertThat(result).isNotNull
        assertThat(result.success).isTrue()
        assertThat(result.data).isEqualTo(response)
        assertThat(result.message).isEqualTo("반응이 토글되었습니다.")
        
        verify(toggleMessageReactionUseCase).toggleReaction(command)
    }
}