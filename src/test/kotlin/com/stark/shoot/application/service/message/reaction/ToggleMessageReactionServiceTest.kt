package com.stark.shoot.application.service.message.reaction

import com.stark.shoot.adapter.`in`.web.dto.message.reaction.ReactionResponse
import com.stark.shoot.domain.chat.message.type.MessageStatus
import com.stark.shoot.domain.chat.message.type.MessageType
import com.stark.shoot.application.port.out.event.EventPublisher
import com.stark.shoot.application.port.out.message.LoadMessagePort
import com.stark.shoot.application.port.out.message.SaveMessagePort
import com.stark.shoot.domain.chat.event.MessageReactionEvent
import com.stark.shoot.domain.chat.message.ChatMessage
import com.stark.shoot.domain.chat.message.MessageContent
import com.stark.shoot.domain.chat.message.ReactionToggleResult
import com.stark.shoot.domain.chat.reaction.MessageReactions
import com.stark.shoot.domain.chat.reaction.ReactionType
import com.stark.shoot.infrastructure.exception.web.InvalidInputException
import com.stark.shoot.infrastructure.exception.web.ResourceNotFoundException
import com.stark.shoot.infrastructure.util.toObjectId
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

    private val loadMessagePort = mock(LoadMessagePort::class.java)
    private val saveMessagePort = mock(SaveMessagePort::class.java)
    private val messagingTemplate = mock(SimpMessagingTemplate::class.java)
    private val eventPublisher = mock(EventPublisher::class.java)
    
    private val toggleMessageReactionService = ToggleMessageReactionService(
        loadMessagePort,
        saveMessagePort,
        messagingTemplate,
        eventPublisher
    )

    @Nested
    @DisplayName("메시지 리액션 토글 시")
    inner class ToggleReaction {

        @Test
        @DisplayName("유효한 리액션 타입으로 리액션을 추가할 수 있다")
        fun `유효한 리액션 타입으로 리액션을 추가할 수 있다`() {
            // given
            val messageId = "5f9f1b9b9c9d1b9b9c9d1b9b"
            val userId = 1L
            val reactionType = "like"
            val roomId = 1L
            val now = Instant.now()
            
            val initialReactions = mapOf<String, Set<Long>>()
            val updatedReactions = mapOf(reactionType to setOf(userId))
            
            val message = ChatMessage(
                id = messageId,
                roomId = roomId,
                senderId = 2L,
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
                messageId = savedMessage.id ?: messageId,
                reactions = savedMessage.reactions,
                updatedAt = savedMessage.updatedAt?.toString() ?: ""
            )
            
            `when`(loadMessagePort.findById(messageId.toObjectId())).thenReturn(message)
            
            // Mock the toggleReaction method on ChatMessage
            val spyMessage = spy(message)
            `when`(spyMessage.toggleReaction(userId, ReactionType.LIKE)).thenReturn(toggleResult)
            `when`(loadMessagePort.findById(messageId.toObjectId())).thenReturn(spyMessage)
            
            `when`(saveMessagePort.save(toggleResult.message)).thenReturn(savedMessage)
            
            // when
            val result = toggleMessageReactionService.toggleReaction(messageId, userId, reactionType)
            
            // then
            assertThat(result.messageId).isEqualTo(expectedResponse.messageId)
            assertThat(result.reactions).isEqualTo(expectedResponse.reactions)
            
            verify(loadMessagePort).findById(messageId.toObjectId())
            verify(saveMessagePort).save(toggleResult.message)
        }
        
        @Test
        @DisplayName("존재하지 않는 메시지 ID로 토글하면 예외가 발생한다")
        fun `존재하지 않는 메시지 ID로 토글하면 예외가 발생한다`() {
            // given
            val messageId = "5f9f1b9b9c9d1b9b9c9d1b9b"
            val userId = 1L
            val reactionType = "like"
            
            `when`(loadMessagePort.findById(messageId.toObjectId())).thenReturn(null)
            
            // when & then
            assertThrows<ResourceNotFoundException> {
                toggleMessageReactionService.toggleReaction(messageId, userId, reactionType)
            }
            
            verify(loadMessagePort).findById(messageId.toObjectId())
            verifyNoInteractions(saveMessagePort)
            verifyNoInteractions(messagingTemplate)
            verifyNoInteractions(eventPublisher)
        }
        
        @Test
        @DisplayName("지원하지 않는 리액션 타입으로 토글하면 예외가 발생한다")
        fun `지원하지 않는 리액션 타입으로 토글하면 예외가 발생한다`() {
            // given
            val messageId = "5f9f1b9b9c9d1b9b9c9d1b9b"
            val userId = 1L
            val invalidReactionType = "invalid_type"
            
            // when & then
            assertThrows<InvalidInputException> {
                toggleMessageReactionService.toggleReaction(messageId, userId, invalidReactionType)
            }
            
            verifyNoInteractions(loadMessagePort)
            verifyNoInteractions(saveMessagePort)
            verifyNoInteractions(messagingTemplate)
            verifyNoInteractions(eventPublisher)
        }
    }
}