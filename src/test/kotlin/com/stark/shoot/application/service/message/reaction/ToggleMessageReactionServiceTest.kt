package com.stark.shoot.application.service.message.reaction

import com.stark.shoot.adapter.`in`.rest.dto.message.reaction.ReactionResponse
import com.stark.shoot.application.port.`in`.message.reaction.command.ToggleMessageReactionCommand
import com.stark.shoot.application.port.out.event.EventPublisher
import com.stark.shoot.application.port.out.message.MessageCommandPort
import com.stark.shoot.application.port.out.message.MessageQueryPort
import com.stark.shoot.domain.chat.message.ChatMessage
import com.stark.shoot.domain.chat.message.service.MessageReactionService
import com.stark.shoot.domain.chat.message.type.MessageStatus
import com.stark.shoot.domain.chat.message.type.MessageType
import com.stark.shoot.domain.chat.message.vo.MessageContent
import com.stark.shoot.domain.chat.message.vo.MessageId
import com.stark.shoot.domain.chat.message.vo.ReactionToggleResult
import com.stark.shoot.domain.chat.reaction.type.ReactionType
import com.stark.shoot.domain.chat.reaction.vo.MessageReactions
import com.stark.shoot.domain.chatroom.vo.ChatRoomId
import com.stark.shoot.domain.user.vo.UserId
import com.stark.shoot.infrastructure.exception.web.InvalidInputException
import com.stark.shoot.infrastructure.exception.web.ResourceNotFoundException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.*
import org.springframework.messaging.simp.SimpMessagingTemplate
import java.time.Instant

@DisplayName("메시지 리액션 토글 서비스 테스트")
class ToggleMessageReactionServiceTest {

    private val messageQueryPort = mock(MessageQueryPort::class.java)
    private val messageCommandPort = mock(MessageCommandPort::class.java)
    private val messagingTemplate = mock(SimpMessagingTemplate::class.java)
    private val eventPublisher = mock(EventPublisher::class.java)
    private val messageReactionService = mock(MessageReactionService::class.java)

    private val toggleMessageReactionService = ToggleMessageReactionService(
        messageQueryPort,
        messageCommandPort,
        messagingTemplate,
        eventPublisher,
        messageReactionService
    )

    @Nested
    @DisplayName("메시지 리액션 토글 시")
    inner class ToggleReaction {

        @Test
        @DisplayName("[happy] 유효한 리액션 타입으로 리액션을 추가할 수 있다")
        fun `유효한 리액션 타입으로 리액션을 추가할 수 있다`() {
            // given
            val messageIdStr = "5f9f1b9b9c9d1b9b9c9d1b9b"
            val messageId = MessageId.from(messageIdStr)
            val userIdLong = 1L
            val userId = UserId.from(userIdLong)
            val reactionType = "like"
            val roomIdLong = 1L
            val roomId = ChatRoomId.from(roomIdLong)
            val now = Instant.now()

            val initialReactions = mapOf<String, Set<Long>>()
            val updatedReactions = mapOf(reactionType to setOf(userIdLong))

            val message = ChatMessage(
                id = messageId,
                roomId = roomId,
                senderId = UserId.from(2L),
                content = MessageContent("테스트 메시지", MessageType.TEXT),
                status = MessageStatus.SAVED,
                messageReactions = MessageReactions(initialReactions),
                createdAt = now,
                updatedAt = now
            )

            val updatedMessage = message.copy(
                messageReactions = MessageReactions(updatedReactions),
                updatedAt = now
            )

            val toggleResult = ReactionToggleResult(
                reactions = MessageReactions(updatedReactions),
                message = updatedMessage,
                userId = userId,
                reactionType = reactionType,
                isAdded = true,
                previousReactionType = null,
                isReplacement = false
            )

            val savedMessage = updatedMessage.copy()

            val expectedResponse = ReactionResponse.from(
                messageId = savedMessage.id?.value ?: messageIdStr,
                reactions = savedMessage.reactions,
                updatedAt = savedMessage.updatedAt?.toString() ?: ""
            )

            `when`(messageQueryPort.findById(messageId)).thenReturn(message)

            // Mock the toggleReaction method on ChatMessage
            val spyMessage = spy(message)
            `when`(spyMessage.toggleReaction(userId, ReactionType.LIKE)).thenReturn(toggleResult)
            `when`(messageQueryPort.findById(messageId)).thenReturn(spyMessage)

            `when`(messageCommandPort.save(toggleResult.message)).thenReturn(savedMessage)

            // when
            val command = ToggleMessageReactionCommand(messageId, userId, reactionType)
            val result = toggleMessageReactionService.toggleReaction(command)

            // then
            assertThat(result.messageId).isEqualTo(expectedResponse.messageId)
            assertThat(result.reactions).isEqualTo(expectedResponse.reactions)

            verify(messageQueryPort).findById(messageId)
            verify(messageCommandPort).save(toggleResult.message)
        }

        @Test
        @DisplayName("[bad] 존재하지 않는 메시지 ID로 토글하면 예외가 발생한다")
        fun `존재하지 않는 메시지 ID로 토글하면 예외가 발생한다`() {
            // given
            val messageIdStr = "5f9f1b9b9c9d1b9b9c9d1b9b"
            val messageId = MessageId.from(messageIdStr)
            val userIdLong = 1L
            val userId = UserId.from(userIdLong)
            val reactionType = "like"

            `when`(messageQueryPort.findById(messageId)).thenReturn(null)

            // when & then
            val command = ToggleMessageReactionCommand(messageId, userId, reactionType)
            assertThrows<ResourceNotFoundException> {
                toggleMessageReactionService.toggleReaction(command)
            }

            verify(messageQueryPort).findById(messageId)
            verifyNoInteractions(messageCommandPort)
            verifyNoInteractions(messagingTemplate)
            verifyNoInteractions(eventPublisher)
        }

        @Test
        @DisplayName("[bad] 지원하지 않는 리액션 타입으로 토글하면 예외가 발생한다")
        fun `지원하지 않는 리액션 타입으로 토글하면 예외가 발생한다`() {
            // given
            val messageIdStr = "5f9f1b9b9c9d1b9b9c9d1b9b"
            val messageId = MessageId.from(messageIdStr)
            val userIdLong = 1L
            val userId = UserId.from(userIdLong)
            val invalidReactionType = "invalid_type"

            // when & then
            val command = ToggleMessageReactionCommand(messageId, userId, invalidReactionType)
            assertThrows<InvalidInputException> {
                toggleMessageReactionService.toggleReaction(command)
            }

            verifyNoInteractions(messageQueryPort)
            verifyNoInteractions(messageCommandPort)
            verifyNoInteractions(messagingTemplate)
            verifyNoInteractions(eventPublisher)
        }
    }
}
