package com.stark.shoot.application.service.message.reaction

import com.stark.shoot.application.port.out.message.LoadMessagePort
import com.stark.shoot.domain.chat.message.ChatMessage
import com.stark.shoot.domain.chat.message.type.MessageStatus
import com.stark.shoot.domain.chat.message.type.MessageType
import com.stark.shoot.domain.chat.message.vo.MessageContent
import com.stark.shoot.domain.chat.reaction.type.ReactionType
import com.stark.shoot.domain.chat.reaction.vo.MessageReactions
import com.stark.shoot.infrastructure.exception.web.ResourceNotFoundException
import com.stark.shoot.infrastructure.util.toObjectId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.*

@DisplayName("메시지 리액션 조회 서비스 테스트")
class GetMessageReactionServiceTest {

    private val loadMessagePort = mock(LoadMessagePort::class.java)
    private val getMessageReactionService = GetMessageReactionService(loadMessagePort)

    @Nested
    @DisplayName("메시지 리액션 조회 시")
    inner class GetReactions {

        @Test
        @DisplayName("메시지 ID로 리액션을 조회할 수 있다")
        fun `메시지 ID로 리액션을 조회할 수 있다`() {
            // given
            val messageId = "5f9f1b9b9c9d1b9b9c9d1b9b"
            val reactions = mapOf(
                "like" to setOf(1L, 2L),
                "heart" to setOf(3L)
            )
            val message = ChatMessage(
                id = messageId,
                roomId = 1L,
                senderId = 1L,
                content = MessageContent("테스트 메시지", MessageType.TEXT),
                status = MessageStatus.SAVED,
                messageReactions = MessageReactions(reactions)
            )

            `when`(loadMessagePort.findById(messageId.toObjectId())).thenReturn(message)

            // when
            val result = getMessageReactionService.getReactions(messageId)

            // then
            assertThat(result).isEqualTo(reactions)
            verify(loadMessagePort).findById(messageId.toObjectId())
        }

        @Test
        @DisplayName("존재하지 않는 메시지 ID로 조회하면 예외가 발생한다")
        fun `존재하지 않는 메시지 ID로 조회하면 예외가 발생한다`() {
            // given
            val messageId = "5f9f1b9b9c9d1b9b9c9d1b9b"

            `when`(loadMessagePort.findById(messageId.toObjectId())).thenReturn(null)

            // when & then
            assertThrows<ResourceNotFoundException> {
                getMessageReactionService.getReactions(messageId)
            }
            verify(loadMessagePort).findById(messageId.toObjectId())
        }
    }

    @Nested
    @DisplayName("지원하는 리액션 타입 조회 시")
    inner class GetSupportedReactionTypes {

        @Test
        @DisplayName("지원하는 모든 리액션 타입을 조회할 수 있다")
        fun `지원하는 모든 리액션 타입을 조회할 수 있다`() {
            // when
            val result = getMessageReactionService.getSupportedReactionTypes()

            // then
            assertThat(result).containsExactlyInAnyOrderElementsOf(ReactionType.entries)
        }
    }
}