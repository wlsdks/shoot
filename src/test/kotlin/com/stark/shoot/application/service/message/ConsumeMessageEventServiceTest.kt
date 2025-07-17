package com.stark.shoot.application.service.message

import com.stark.shoot.adapter.`in`.web.socket.WebSocketMessageBroker
import com.stark.shoot.adapter.out.persistence.mongodb.mapper.ChatMessageMapper
import com.stark.shoot.application.port.out.chatroom.ChatRoomCommandPort
import com.stark.shoot.application.port.out.chatroom.ChatRoomQueryPort
import com.stark.shoot.application.port.out.event.EventPublisher
import com.stark.shoot.application.port.out.message.SaveMessagePort
import com.stark.shoot.application.port.out.message.preview.CacheUrlPreviewPort
import com.stark.shoot.application.port.out.message.preview.LoadUrlContentPort
import com.stark.shoot.domain.chatroom.service.ChatRoomMetadataDomainService
import com.stark.shoot.domain.chat.message.ChatMessage
import com.stark.shoot.domain.chat.message.type.MessageStatus
import com.stark.shoot.domain.chat.message.type.MessageType
import com.stark.shoot.domain.chat.message.vo.ChatMessageMetadata
import com.stark.shoot.domain.chat.message.vo.MessageContent
import com.stark.shoot.domain.chat.message.vo.MessageId
import com.stark.shoot.domain.chatroom.vo.ChatRoomId
import com.stark.shoot.domain.event.MessageEvent
import com.stark.shoot.domain.event.type.EventType
import com.stark.shoot.domain.user.vo.UserId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import java.time.Instant
import java.util.concurrent.CompletableFuture

@DisplayName("ConsumeMessageEventService 테스트")
class ConsumeMessageEventServiceTest {

    @Test
    @DisplayName("[happy] 이벤트를 성공적으로 처리하면 true를 반환한다")
    fun `이벤트를 성공적으로 처리하면 true를 반환한다`() {
        val saveMessagePort = mock(SaveMessagePort::class.java)
        val chatRoomQueryPort = mock(ChatRoomQueryPort::class.java)
        val chatRoomCommandPort = mock(ChatRoomCommandPort::class.java)
        val eventPublisher = mock(EventPublisher::class.java)
        val chatRoomMetadataDomainService = mock(ChatRoomMetadataDomainService::class.java)
        val loadUrlContentPort = mock(LoadUrlContentPort::class.java)
        val cacheUrlPreviewPort = mock(CacheUrlPreviewPort::class.java)
        val webSocketMessageBroker = mock(WebSocketMessageBroker::class.java)
        val chatMessageMapper = mock(ChatMessageMapper::class.java)

        val service = ConsumeMessageEventService(
            saveMessagePort,
            chatRoomQueryPort,
            chatRoomCommandPort,
            eventPublisher,
            chatRoomMetadataDomainService,
            loadUrlContentPort,
            cacheUrlPreviewPort,
            webSocketMessageBroker,
            chatMessageMapper
        )

        val message = ChatMessage(
            id = MessageId.from("m1"),
            roomId = ChatRoomId.from(1L),
            senderId = UserId.from(2L),
            content = MessageContent("hi", MessageType.TEXT),
            status = MessageStatus.SENDING,
            createdAt = Instant.now(),
            metadata = ChatMessageMetadata(tempId = "t1")
        )

        val event = MessageEvent.fromMessage(message, EventType.MESSAGE_CREATED)

        `when`(saveMessagePort.save(message)).thenReturn(message)
        `when`(webSocketMessageBroker.sendMessage(anyString(), any())).thenReturn(CompletableFuture.completedFuture(true))

        val result = service.consume(event)

        assertThat(result).isTrue()
        verify(saveMessagePort).save(message)
    }

    @Test
    @DisplayName("[bad] 처리 중 예외가 발생하면 false를 반환한다")
    fun `처리 중 예외가 발생하면 false를 반환한다`() {
        val saveMessagePort = mock(SaveMessagePort::class.java)
        val chatRoomQueryPort = mock(ChatRoomQueryPort::class.java)
        val chatRoomCommandPort = mock(ChatRoomCommandPort::class.java)
        val eventPublisher = mock(EventPublisher::class.java)
        val chatRoomMetadataDomainService = mock(ChatRoomMetadataDomainService::class.java)
        val loadUrlContentPort = mock(LoadUrlContentPort::class.java)
        val cacheUrlPreviewPort = mock(CacheUrlPreviewPort::class.java)
        val webSocketMessageBroker = mock(WebSocketMessageBroker::class.java)
        val chatMessageMapper = mock(ChatMessageMapper::class.java)

        val service = ConsumeMessageEventService(
            saveMessagePort,
            chatRoomQueryPort,
            chatRoomCommandPort,
            eventPublisher,
            chatRoomMetadataDomainService,
            loadUrlContentPort,
            cacheUrlPreviewPort,
            webSocketMessageBroker,
            chatMessageMapper
        )

        val message = ChatMessage(
            id = MessageId.from("m2"),
            roomId = ChatRoomId.from(1L),
            senderId = UserId.from(2L),
            content = MessageContent("hi", MessageType.TEXT),
            status = MessageStatus.SENDING,
            createdAt = Instant.now(),
            metadata = ChatMessageMetadata(tempId = "t2")
        )

        val event = MessageEvent.fromMessage(message, EventType.MESSAGE_CREATED)

        `when`(saveMessagePort.save(any())).thenThrow(RuntimeException("fail"))
        `when`(webSocketMessageBroker.sendMessage(anyString(), any())).thenReturn(CompletableFuture.completedFuture(true))

        val result = service.consume(event)

        assertThat(result).isFalse()
        verify(saveMessagePort).save(any())
    }
}
