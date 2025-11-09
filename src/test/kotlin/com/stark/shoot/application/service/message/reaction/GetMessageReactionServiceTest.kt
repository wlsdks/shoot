package com.stark.shoot.application.service.message.reaction

import com.stark.shoot.application.port.`in`.message.reaction.command.GetMessageReactionsCommand
import com.stark.shoot.application.port.out.message.MessageQueryPort
import com.stark.shoot.application.port.out.message.reaction.MessageReactionQueryPort
import com.stark.shoot.domain.chat.message.ChatMessage
import com.stark.shoot.domain.chat.message.type.MessageStatus
import com.stark.shoot.domain.chat.message.type.MessageType
import com.stark.shoot.domain.chat.message.vo.MessageContent
import com.stark.shoot.domain.chat.message.vo.MessageId
import com.stark.shoot.domain.chat.reaction.MessageReaction
import com.stark.shoot.domain.chat.reaction.type.ReactionType
import com.stark.shoot.domain.chat.vo.ChatRoomId
import com.stark.shoot.domain.shared.UserId
import com.stark.shoot.infrastructure.exception.web.ResourceNotFoundException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.*

@DisplayName("메시지 리액션 조회 서비스 테스트 (MessageReaction Aggregate)")
class GetMessageReactionServiceTest {

    private val messageQueryPort = mock(MessageQueryPort::class.java)
    private val messageReactionQueryPort = mock(MessageReactionQueryPort::class.java)
    private val getMessageReactionService = GetMessageReactionService(
        messageQueryPort,
        messageReactionQueryPort
    )

    @Nested
    @DisplayName("메시지 리액션 조회 시")
    inner class GetReactions {

        @Test
        @DisplayName("[happy] 메시지 ID로 리액션을 조회할 수 있다")
        fun `메시지 ID로 리액션을 조회할 수 있다`() {
            // given
            val messageId = MessageId.from("msg-123")
            val message = ChatMessage(
                id = messageId,
                roomId = ChatRoomId.from(1L),
                senderId = UserId.from(1L),
                content = MessageContent("테스트 메시지", MessageType.TEXT),
                status = MessageStatus.SENT
            )

            // MessageReaction Aggregate 목록
            val reactions = listOf(
                MessageReaction.create(messageId, UserId.from(1L), ReactionType.LIKE),
                MessageReaction.create(messageId, UserId.from(2L), ReactionType.LIKE),
                MessageReaction.create(messageId, UserId.from(3L), ReactionType.SAD)
            )

            `when`(messageQueryPort.findById(messageId)).thenReturn(message)
            `when`(messageReactionQueryPort.findAllByMessageId(messageId)).thenReturn(reactions)

            // when
            val command = GetMessageReactionsCommand(messageId)
            val result = getMessageReactionService.getReactions(command)

            // then
            assertThat(result).containsEntry("like", setOf(1L, 2L))
            assertThat(result).containsEntry("sad", setOf(3L))
            verify(messageQueryPort).findById(messageId)
            verify(messageReactionQueryPort).findAllByMessageId(messageId)
        }

        @Test
        @DisplayName("[happy] 리액션이 없는 경우 빈 맵을 반환한다")
        fun `리액션이 없는 경우 빈 맵을 반환한다`() {
            // given
            val messageId = MessageId.from("msg-123")
            val message = ChatMessage(
                id = messageId,
                roomId = ChatRoomId.from(1L),
                senderId = UserId.from(1L),
                content = MessageContent("테스트 메시지", MessageType.TEXT),
                status = MessageStatus.SENT
            )

            `when`(messageQueryPort.findById(messageId)).thenReturn(message)
            `when`(messageReactionQueryPort.findAllByMessageId(messageId)).thenReturn(emptyList())

            // when
            val command = GetMessageReactionsCommand(messageId)
            val result = getMessageReactionService.getReactions(command)

            // then
            assertThat(result).isEmpty()
            verify(messageQueryPort).findById(messageId)
            verify(messageReactionQueryPort).findAllByMessageId(messageId)
        }

        @Test
        @DisplayName("[bad] 존재하지 않는 메시지 ID로 조회하면 예외가 발생한다")
        fun `존재하지 않는 메시지 ID로 조회하면 예외가 발생한다`() {
            // given
            val messageId = MessageId.from("msg-nonexistent")

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
