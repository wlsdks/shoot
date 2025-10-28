package com.stark.shoot.application.service.message.pin

import com.stark.shoot.adapter.`in`.socket.WebSocketMessageBroker
import com.stark.shoot.application.port.`in`.message.pin.command.PinMessageCommand
import com.stark.shoot.application.port.`in`.message.pin.command.UnpinMessageCommand
import com.stark.shoot.application.port.out.event.EventPublishPort
import com.stark.shoot.application.port.out.message.MessageCommandPort
import com.stark.shoot.application.port.out.message.MessageQueryPort
import com.stark.shoot.domain.chat.message.ChatMessage
import com.stark.shoot.domain.chat.message.service.MessagePinDomainService
import com.stark.shoot.domain.chat.message.type.MessageStatus
import com.stark.shoot.domain.chat.message.type.MessageType
import com.stark.shoot.domain.chat.message.vo.MessageContent
import com.stark.shoot.domain.chat.message.vo.MessageId
import com.stark.shoot.domain.chatroom.vo.ChatRoomId
import com.stark.shoot.domain.user.vo.UserId
import com.stark.shoot.infrastructure.exception.web.ResourceNotFoundException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.*
import java.time.Instant

@DisplayName("메시지 고정 서비스 테스트")
class MessagePinServiceTest {

    @Test
    @DisplayName("[happy] 존재하지 않는 메시지를 고정하려고 하면 예외가 발생한다")
    fun `존재하지 않는 메시지를 고정하려고 하면 예외가 발생한다`() {
        // given
        val messageQueryPort = mock(MessageQueryPort::class.java)
        val messageCommandPort = mock(MessageCommandPort::class.java)
        val webSocketMessageBroker = mock(WebSocketMessageBroker::class.java)
        val eventPublisher = mock(EventPublishPort::class.java)
        val messagePinDomainService = mock(MessagePinDomainService::class.java)
        val domainConstants = mock(com.stark.shoot.infrastructure.config.domain.DomainConstants::class.java)
        val chatRoomConstants = com.stark.shoot.infrastructure.config.domain.DomainConstants.ChatRoomConstants(maxPinnedMessages = 5)
        `when`(domainConstants.chatRoom).thenReturn(chatRoomConstants)

        val messagePinService = MessagePinService(
            messageQueryPort,
            messageCommandPort,
            webSocketMessageBroker,
            eventPublisher,
            messagePinDomainService,
            domainConstants
        )

        val messageId = MessageId.from("5f9f1b9b9c9d1b9b9c9d1b9b")
        val userId = UserId.from(1L)

        `when`(messageQueryPort.findById(messageId)).thenReturn(null)

        // when & then
        val command = PinMessageCommand(messageId, userId)
        assertThrows<ResourceNotFoundException> {
            messagePinService.pinMessage(command)
        }

        verify(messageQueryPort).findById(messageId)
        verifyNoMoreInteractions(messageQueryPort)
        verifyNoInteractions(messageCommandPort)
        verifyNoInteractions(webSocketMessageBroker)
        verifyNoInteractions(eventPublisher)
    }

    @Test
    @DisplayName("[happy] 이미 고정 해제된 메시지는 변경 없이 반환한다")
    fun `이미 고정 해제된 메시지는 변경 없이 반환한다`() {
        // given
        val messageQueryPort = mock(MessageQueryPort::class.java)
        val messageCommandPort = mock(MessageCommandPort::class.java)
        val webSocketMessageBroker = mock(WebSocketMessageBroker::class.java)
        val eventPublisher = mock(EventPublishPort::class.java)
        val messagePinDomainService = mock(MessagePinDomainService::class.java)
        val domainConstants = mock(com.stark.shoot.infrastructure.config.domain.DomainConstants::class.java)
        val chatRoomConstants = com.stark.shoot.infrastructure.config.domain.DomainConstants.ChatRoomConstants(maxPinnedMessages = 5)
        `when`(domainConstants.chatRoom).thenReturn(chatRoomConstants)

        val messagePinService = MessagePinService(
            messageQueryPort,
            messageCommandPort,
            webSocketMessageBroker,
            eventPublisher,
            messagePinDomainService,
            domainConstants
        )

        val messageId = MessageId.from("5f9f1b9b9c9d1b9b9c9d1b9b")
        val userId = UserId.from(1L)
        val roomId = ChatRoomId.from(2L)

        val unpinnedMessage = ChatMessage(
            id = messageId,
            roomId = roomId,
            senderId = UserId.from(3L),
            content = MessageContent("고정되지 않은 메시지", MessageType.TEXT),
            status = MessageStatus.SENT,
            createdAt = Instant.now(),
            isPinned = false
        )

        // Mock the message retrieval
        `when`(messageQueryPort.findById(messageId)).thenReturn(unpinnedMessage)

        // when
        val command = UnpinMessageCommand(messageId, userId)
        val result = messagePinService.unpinMessage(command)

        // then
        assertThat(result).isEqualTo(unpinnedMessage)
        assertThat(result.isPinned).isFalse()

        verify(messageQueryPort).findById(messageId)
        verifyNoMoreInteractions(messageQueryPort)
        verifyNoInteractions(messageCommandPort)
        verifyNoInteractions(webSocketMessageBroker)
        verifyNoInteractions(eventPublisher)
    }

    @Test
    @DisplayName("[happy] 존재하지 않는 메시지를 해제하려고 하면 예외가 발생한다")
    fun `존재하지 않는 메시지를 해제하려고 하면 예외가 발생한다`() {
        // given
        val messageQueryPort = mock(MessageQueryPort::class.java)
        val messageCommandPort = mock(MessageCommandPort::class.java)
        val webSocketMessageBroker = mock(WebSocketMessageBroker::class.java)
        val eventPublisher = mock(EventPublishPort::class.java)
        val messagePinDomainService = mock(MessagePinDomainService::class.java)
        val domainConstants = mock(com.stark.shoot.infrastructure.config.domain.DomainConstants::class.java)
        val chatRoomConstants = com.stark.shoot.infrastructure.config.domain.DomainConstants.ChatRoomConstants(maxPinnedMessages = 5)
        `when`(domainConstants.chatRoom).thenReturn(chatRoomConstants)

        val messagePinService = MessagePinService(
            messageQueryPort,
            messageCommandPort,
            webSocketMessageBroker,
            eventPublisher,
            messagePinDomainService,
            domainConstants
        )

        val messageId = MessageId.from("5f9f1b9b9c9d1b9b9c9d1b9b")
        val userId = UserId.from(1L)

        `when`(messageQueryPort.findById(messageId)).thenReturn(null)

        // when & then
        val command = UnpinMessageCommand(messageId, userId)
        assertThrows<ResourceNotFoundException> {
            messagePinService.unpinMessage(command)
        }

        verify(messageQueryPort).findById(messageId)
        verifyNoMoreInteractions(messageQueryPort)
        verifyNoInteractions(messageCommandPort)
        verifyNoInteractions(webSocketMessageBroker)
        verifyNoInteractions(eventPublisher)
    }
}
