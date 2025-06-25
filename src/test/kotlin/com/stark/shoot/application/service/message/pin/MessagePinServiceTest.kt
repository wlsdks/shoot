package com.stark.shoot.application.service.message.pin

import com.stark.shoot.adapter.`in`.web.socket.WebSocketMessageBroker
import com.stark.shoot.application.port.out.event.EventPublisher
import com.stark.shoot.application.port.out.message.MessageQueryPort
import com.stark.shoot.application.port.out.message.MessageCommandPort
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
        val eventPublisher = mock(EventPublisher::class.java)
        val messagePinDomainService = mock(MessagePinDomainService::class.java)

        val messagePinService = MessagePinService(
            messageQueryPort,
            messageCommandPort,
            webSocketMessageBroker,
            eventPublisher,
            messagePinDomainService
        )

        val messageId = MessageId.from("5f9f1b9b9c9d1b9b9c9d1b9b")
        val userId = UserId.from(1L)

        `when`(messageQueryPort.findById(messageId)).thenReturn(null)

        // when & then
        assertThrows<ResourceNotFoundException> {
            messagePinService.pinMessage(messageId, userId)
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
        val eventPublisher = mock(EventPublisher::class.java)
        val messagePinDomainService = mock(MessagePinDomainService::class.java)

        val messagePinService = MessagePinService(
            messageQueryPort,
            messageCommandPort,
            webSocketMessageBroker,
            eventPublisher,
            messagePinDomainService
        )

        val messageId = MessageId.from("5f9f1b9b9c9d1b9b9c9d1b9b")
        val userId = UserId.from(1L)
        val roomId = ChatRoomId.from(2L)

        val unpinnedMessage = ChatMessage(
            id = messageId,
            roomId = roomId,
            senderId = UserId.from(3L),
            content = MessageContent("고정되지 않은 메시지", MessageType.TEXT),
            status = MessageStatus.SAVED,
            createdAt = Instant.now(),
            isPinned = false
        )

        // Mock the message retrieval
        `when`(messageQueryPort.findById(messageId)).thenReturn(unpinnedMessage)

        // when
        val result = messagePinService.unpinMessage(messageId, userId)

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
        val eventPublisher = mock(EventPublisher::class.java)
        val messagePinDomainService = mock(MessagePinDomainService::class.java)

        val messagePinService = MessagePinService(
            messageQueryPort,
            messageCommandPort,
            webSocketMessageBroker,
            eventPublisher,
            messagePinDomainService
        )

        val messageId = MessageId.from("5f9f1b9b9c9d1b9b9c9d1b9b")
        val userId = UserId.from(1L)

        `when`(messageQueryPort.findById(messageId)).thenReturn(null)

        // when & then
        assertThrows<ResourceNotFoundException> {
            messagePinService.unpinMessage(messageId, userId)
        }

        verify(messageQueryPort).findById(messageId)
        verifyNoMoreInteractions(messageQueryPort)
        verifyNoInteractions(messageCommandPort)
        verifyNoInteractions(webSocketMessageBroker)
        verifyNoInteractions(eventPublisher)
    }
}
