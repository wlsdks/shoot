package com.stark.shoot.application.service.message

import com.stark.shoot.application.port.out.message.MessageStatusNotificationPort
import com.stark.shoot.application.port.out.chatroom.ChatRoomCommandPort
import com.stark.shoot.application.port.out.chatroom.ChatRoomQueryPort
import com.stark.shoot.application.port.out.event.EventPublishPort
import com.stark.shoot.application.port.out.message.SaveMessagePort
import com.stark.shoot.application.port.out.message.preview.CacheUrlPreviewPort
import com.stark.shoot.application.port.out.message.preview.LoadUrlContentPort
import com.stark.shoot.domain.chat.message.ChatMessage
import com.stark.shoot.domain.chat.message.type.MessageStatus
import com.stark.shoot.domain.chat.message.type.MessageType
import com.stark.shoot.domain.chat.message.vo.ChatMessageMetadata
import com.stark.shoot.domain.chat.message.vo.MessageContent
import com.stark.shoot.domain.chat.message.vo.MessageId
import com.stark.shoot.domain.chatroom.service.ChatRoomMetadataDomainService
import com.stark.shoot.domain.chatroom.vo.ChatRoomId
import com.stark.shoot.domain.event.MessageEvent
import com.stark.shoot.domain.event.type.EventType
import com.stark.shoot.domain.user.vo.UserId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import java.time.Instant

@DisplayName("HandleMessageEventService 테스트")
class HandleMessageEventServiceTest {

    @Test
    @DisplayName("[happy] 이벤트를 성공적으로 처리하면 true를 반환한다")
    fun `이벤트를 성공적으로 처리하면 true를 반환한다`() {
        // Create lenient mocks to avoid strict verification issues
        val saveMessagePort = mock(SaveMessagePort::class.java, withSettings().lenient())
        val chatRoomQueryPort = mock(ChatRoomQueryPort::class.java, withSettings().lenient())
        val chatRoomCommandPort = mock(ChatRoomCommandPort::class.java, withSettings().lenient())
        val eventPublisher = mock(EventPublishPort::class.java, withSettings().lenient())
        val chatRoomMetadataDomainService = mock(ChatRoomMetadataDomainService::class.java, withSettings().lenient())
        val loadUrlContentPort = mock(LoadUrlContentPort::class.java, withSettings().lenient())
        val cacheUrlPreviewPort = mock(CacheUrlPreviewPort::class.java, withSettings().lenient())
        val messageStatusNotificationPort = mock(MessageStatusNotificationPort::class.java, withSettings().lenient())

        val service = HandleMessageEventService(
            saveMessagePort,
            chatRoomQueryPort,
            chatRoomCommandPort,
            loadUrlContentPort,
            cacheUrlPreviewPort,
            messageStatusNotificationPort,
            chatRoomMetadataDomainService,
            eventPublisher
        )

        val message = ChatMessage(
            id = MessageId.from("m1"),
            roomId = ChatRoomId.from(1L),
            senderId = UserId.from(2L),
            content = MessageContent("hi", MessageType.TEXT),
            status = MessageStatus.SENT,
            createdAt = Instant.now(),
            metadata = ChatMessageMetadata(tempId = "t1")
        )

        val event = MessageEvent.fromMessage(message, EventType.MESSAGE_CREATED)

        // Simple setup - just return the message when saved
        `when`(saveMessagePort.save(message)).thenReturn(message)

        val result = service.handle(event)

        assertThat(result).isTrue()
    }

    @Test
    @DisplayName("[bad] 처리 중 예외가 발생하면 false를 반환한다")
    fun `처리 중 예외가 발생하면 false를 반환한다`() {
        // Create lenient mocks
        val saveMessagePort = mock(SaveMessagePort::class.java, withSettings().lenient())
        val chatRoomQueryPort = mock(ChatRoomQueryPort::class.java, withSettings().lenient())
        val chatRoomCommandPort = mock(ChatRoomCommandPort::class.java, withSettings().lenient())
        val eventPublisher = mock(EventPublishPort::class.java, withSettings().lenient())
        val chatRoomMetadataDomainService = mock(ChatRoomMetadataDomainService::class.java, withSettings().lenient())
        val loadUrlContentPort = mock(LoadUrlContentPort::class.java, withSettings().lenient())
        val cacheUrlPreviewPort = mock(CacheUrlPreviewPort::class.java, withSettings().lenient())
        val messageStatusNotificationPort = mock(MessageStatusNotificationPort::class.java, withSettings().lenient())

        val service = HandleMessageEventService(
            saveMessagePort,
            chatRoomQueryPort,
            chatRoomCommandPort,
            loadUrlContentPort,
            cacheUrlPreviewPort,
            messageStatusNotificationPort,
            chatRoomMetadataDomainService,
            eventPublisher
        )

        val message = ChatMessage(
            id = MessageId.from("m2"),
            roomId = ChatRoomId.from(1L),
            senderId = UserId.from(2L),
            content = MessageContent("hi", MessageType.TEXT),
            status = MessageStatus.SENT,
            createdAt = Instant.now(),
            metadata = ChatMessageMetadata(tempId = "t2")
        )

        val event = MessageEvent.fromMessage(message, EventType.MESSAGE_CREATED)

        // Make save operation throw exception
        doThrow(RuntimeException("fail")).`when`(saveMessagePort).save(message)

        val result = service.handle(event)

        assertThat(result).isFalse()
    }
}