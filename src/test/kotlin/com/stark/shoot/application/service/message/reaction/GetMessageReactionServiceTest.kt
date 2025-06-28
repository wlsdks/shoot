package com.stark.shoot.application.service.message.reaction

import com.stark.shoot.application.port.`in`.message.reaction.command.GetMessageReactionsCommand
import com.stark.shoot.application.port.out.message.MessageQueryPort
import com.stark.shoot.domain.chat.message.ChatMessage
import com.stark.shoot.domain.chat.message.type.MessageStatus
import com.stark.shoot.domain.chat.message.type.MessageType
import com.stark.shoot.domain.chat.message.vo.MessageContent
import com.stark.shoot.domain.chat.message.vo.MessageId
import com.stark.shoot.domain.chat.reaction.type.ReactionType
import com.stark.shoot.domain.chat.reaction.vo.MessageReactions
import com.stark.shoot.domain.chatroom.vo.ChatRoomId
import com.stark.shoot.domain.user.vo.UserId
import com.stark.shoot.infrastructure.exception.web.ResourceNotFoundException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.*

@DisplayName("메시지 리액션 조회 서비스 테스트")
class GetMessageReactionServiceTest {

    private val messageQueryPort = mock(MessageQueryPort::class.java)
    private val getMessageReactionService = GetMessageReactionService(messageQueryPort)

    @Nested
    @DisplayName("메시지 리액션 조회 시")
    inner class GetReactions {

        @Test
        @DisplayName("[happy] 메시지 ID로 리액션을 조회할 수 있다")
        fun `메시지 ID로 리액션을 조회할 수 있다`() {
            // given
            val messageId = MessageId.from("5f9f1b9b9c9d1b9b9c9d1b9b")
            val reactions = mapOf(
                "like" to setOf(1L, 2L),
                "heart" to setOf(3L)
            )
            val message = ChatMessage(
                id = messageId,
                roomId = ChatRoomId.from(1L),
                senderId = UserId.from(1L),
                content = MessageContent("테스트 메시지", MessageType.TEXT),
                status = MessageStatus.SAVED,
                messageReactions = MessageReactions(reactions)
            )

            `when`(messageQueryPort.findById(messageId)).thenReturn(message)

            // when
            val command = GetMessageReactionsCommand(messageId)
            val result = getMessageReactionService.getReactions(command)

            // then
            assertThat(result).isEqualTo(reactions)
            verify(messageQueryPort).findById(messageId)
        }

        @Test
        @DisplayName("[bad] 존재하지 않는 메시지 ID로 조회하면 예외가 발생한다")
        fun `존재하지 않는 메시지 ID로 조회하면 예외가 발생한다`() {
            // given
            val messageId = MessageId.from("5f9f1b9b9c9d1b9b9c9d1b9b")

            `when`(messageQueryPort.findById(messageId)).thenReturn(null)

            // when & then
            val command = GetMessageReactionsCommand(messageId)
            assertThrows<ResourceNotFoundException> {
                getMessageReactionService.getReactions(command)
            }
            verify(messageQueryPort).findById(messageId)
        }
    }

    @Nested
    @DisplayName("지원하는 리액션 타입 조회 시")
    inner class GetSupportedReactionTypes {

        @Test
        @DisplayName("[happy] 지원하는 모든 리액션 타입을 조회할 수 있다")
        fun `지원하는 모든 리액션 타입을 조회할 수 있다`() {
            // when
            val result = getMessageReactionService.getSupportedReactionTypes()

            // then
            assertThat(result).containsExactlyInAnyOrderElementsOf(ReactionType.entries)
        }
    }
}
