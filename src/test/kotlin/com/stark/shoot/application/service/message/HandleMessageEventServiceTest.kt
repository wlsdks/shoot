package com.stark.shoot.application.service.message

import com.stark.shoot.adapter.out.persistence.postgres.repository.OutboxEventRepository
import com.stark.shoot.application.port.out.message.MessageStatusNotificationPort
import com.stark.shoot.application.port.out.message.preview.CacheUrlPreviewPort
import com.stark.shoot.application.port.out.message.preview.LoadUrlContentPort
import com.stark.shoot.application.service.saga.message.MessageSagaOrchestrator
import com.stark.shoot.domain.saga.message.MessageSagaContext
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
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.*
import java.time.Instant

@DisplayName("HandleMessageEventService 테스트")
class HandleMessageEventServiceTest {

    @Test
    @DisplayName("[happy] 이벤트를 성공적으로 처리하면 true를 반환한다")
    fun `이벤트를 성공적으로 처리하면 true를 반환한다`() {
        // Create lenient mocks to avoid strict verification issues
        val messageSagaOrchestrator = mock(MessageSagaOrchestrator::class.java, withSettings().lenient())
        val loadUrlContentPort = mock(LoadUrlContentPort::class.java, withSettings().lenient())
        val cacheUrlPreviewPort = mock(CacheUrlPreviewPort::class.java, withSettings().lenient())
        val messageStatusNotificationPort = mock(MessageStatusNotificationPort::class.java, withSettings().lenient())
        val outboxEventRepository = mock(OutboxEventRepository::class.java, withSettings().lenient())

        val service = HandleMessageEventService(
            messageSagaOrchestrator,
            loadUrlContentPort,
            cacheUrlPreviewPort,
            messageStatusNotificationPort,
            outboxEventRepository
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

        // Mock saga orchestrator to return successful context
        val successContext = MessageSagaContext(message = message)
        successContext.markCompleted()
        `when`(messageSagaOrchestrator.execute(ArgumentMatchers.any(ChatMessage::class.java))).thenReturn(successContext)

        val result = service.handle(event)

        assertThat(result).isTrue()
    }

    @Test
    @DisplayName("[bad] 처리 중 예외가 발생하면 false를 반환한다")
    fun `처리 중 예외가 발생하면 false를 반환한다`() {
        // Create lenient mocks
        val messageSagaOrchestrator = mock(MessageSagaOrchestrator::class.java, withSettings().lenient())
        val loadUrlContentPort = mock(LoadUrlContentPort::class.java, withSettings().lenient())
        val cacheUrlPreviewPort = mock(CacheUrlPreviewPort::class.java, withSettings().lenient())
        val messageStatusNotificationPort = mock(MessageStatusNotificationPort::class.java, withSettings().lenient())
        val outboxEventRepository = mock(OutboxEventRepository::class.java, withSettings().lenient())

        val service = HandleMessageEventService(
            messageSagaOrchestrator,
            loadUrlContentPort,
            cacheUrlPreviewPort,
            messageStatusNotificationPort,
            outboxEventRepository
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

        // Mock saga orchestrator to return failed context
        val failedContext = MessageSagaContext(message = message)
        failedContext.markFailed(RuntimeException("Saga failed"))
        `when`(messageSagaOrchestrator.execute(ArgumentMatchers.any(ChatMessage::class.java))).thenReturn(failedContext)

        val result = service.handle(event)

        assertThat(result).isFalse()
    }
}