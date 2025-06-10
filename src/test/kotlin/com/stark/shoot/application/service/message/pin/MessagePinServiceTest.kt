package com.stark.shoot.application.service.message.pin

import com.stark.shoot.adapter.`in`.web.socket.WebSocketMessageBroker
import com.stark.shoot.application.port.out.event.EventPublisher
import com.stark.shoot.application.port.out.message.LoadMessagePort
import com.stark.shoot.application.port.out.message.SaveMessagePort
import com.stark.shoot.domain.chat.message.ChatMessage
import com.stark.shoot.domain.chat.message.MessageContent
import com.stark.shoot.domain.chat.message.PinMessageResult
import com.stark.shoot.domain.chat.message.type.MessageStatus
import com.stark.shoot.domain.chat.message.type.MessageType
import com.stark.shoot.infrastructure.exception.web.ResourceNotFoundException
import com.stark.shoot.infrastructure.util.toObjectId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.*
import java.time.Instant

@DisplayName("메시지 고정 서비스 테스트")
class MessagePinServiceTest {

    @Test
    @DisplayName("존재하지 않는 메시지를 고정하려고 하면 예외가 발생한다")
    fun `존재하지 않는 메시지를 고정하려고 하면 예외가 발생한다`() {
        // given
        val loadMessagePort = mock(LoadMessagePort::class.java)
        val saveMessagePort = mock(SaveMessagePort::class.java)
        val webSocketMessageBroker = mock(WebSocketMessageBroker::class.java)
        val eventPublisher = mock(EventPublisher::class.java)

        val messagePinService = MessagePinService(
            loadMessagePort,
            saveMessagePort,
            webSocketMessageBroker,
            eventPublisher
        )

        val messageId = "5f9f1b9b9c9d1b9b9c9d1b9b"
        val userId = 1L

        `when`(loadMessagePort.findById(messageId.toObjectId())).thenReturn(null)

        // when & then
        assertThrows<ResourceNotFoundException> {
            messagePinService.pinMessage(messageId, userId)
        }

        verify(loadMessagePort).findById(messageId.toObjectId())
        verifyNoMoreInteractions(loadMessagePort)
        verifyNoInteractions(saveMessagePort)
        verifyNoInteractions(webSocketMessageBroker)
        verifyNoInteractions(eventPublisher)
    }

    @Test
    @DisplayName("이미 고정 해제된 메시지는 변경 없이 반환한다")
    fun `이미 고정 해제된 메시지는 변경 없이 반환한다`() {
        // given
        val loadMessagePort = mock(LoadMessagePort::class.java)
        val saveMessagePort = mock(SaveMessagePort::class.java)
        val webSocketMessageBroker = mock(WebSocketMessageBroker::class.java)
        val eventPublisher = mock(EventPublisher::class.java)

        val messagePinService = MessagePinService(
            loadMessagePort,
            saveMessagePort,
            webSocketMessageBroker,
            eventPublisher
        )

        val messageId = "5f9f1b9b9c9d1b9b9c9d1b9b"
        val userId = 1L
        val roomId = 2L

        val unpinnedMessage = ChatMessage(
            id = messageId,
            roomId = roomId,
            senderId = 3L,
            content = MessageContent("고정되지 않은 메시지", MessageType.TEXT),
            status = MessageStatus.SAVED,
            createdAt = Instant.now(),
            isPinned = false
        )

        // Mock the message retrieval
        `when`(loadMessagePort.findById(messageId.toObjectId())).thenReturn(unpinnedMessage)

        // when
        val result = messagePinService.unpinMessage(messageId, userId)

        // then
        assertThat(result).isEqualTo(unpinnedMessage)
        assertThat(result.isPinned).isFalse()

        verify(loadMessagePort).findById(messageId.toObjectId())
        verifyNoMoreInteractions(loadMessagePort)
        verifyNoInteractions(saveMessagePort)
        verifyNoInteractions(webSocketMessageBroker)
        verifyNoInteractions(eventPublisher)
    }

    @Test
    @DisplayName("존재하지 않는 메시지를 해제하려고 하면 예외가 발생한다")
    fun `존재하지 않는 메시지를 해제하려고 하면 예외가 발생한다`() {
        // given
        val loadMessagePort = mock(LoadMessagePort::class.java)
        val saveMessagePort = mock(SaveMessagePort::class.java)
        val webSocketMessageBroker = mock(WebSocketMessageBroker::class.java)
        val eventPublisher = mock(EventPublisher::class.java)

        val messagePinService = MessagePinService(
            loadMessagePort,
            saveMessagePort,
            webSocketMessageBroker,
            eventPublisher
        )

        val messageId = "5f9f1b9b9c9d1b9b9c9d1b9b"
        val userId = 1L

        `when`(loadMessagePort.findById(messageId.toObjectId())).thenReturn(null)

        // when & then
        assertThrows<ResourceNotFoundException> {
            messagePinService.unpinMessage(messageId, userId)
        }

        verify(loadMessagePort).findById(messageId.toObjectId())
        verifyNoMoreInteractions(loadMessagePort)
        verifyNoInteractions(saveMessagePort)
        verifyNoInteractions(webSocketMessageBroker)
        verifyNoInteractions(eventPublisher)
    }
}
