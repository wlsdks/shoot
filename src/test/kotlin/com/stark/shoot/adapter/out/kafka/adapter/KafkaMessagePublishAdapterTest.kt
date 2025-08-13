package com.stark.shoot.adapter.out.kafka.adapter

import com.stark.shoot.adapter.out.kafka.PublishKafkaAdapter
import com.stark.shoot.domain.chat.message.ChatMessage
import com.stark.shoot.domain.chat.message.type.MessageStatus
import com.stark.shoot.domain.chat.message.type.MessageType
import com.stark.shoot.domain.chat.message.vo.MessageContent
import com.stark.shoot.domain.chat.message.vo.MessageId
import com.stark.shoot.domain.chatroom.vo.ChatRoomId
import com.stark.shoot.domain.event.MessageEvent
import com.stark.shoot.domain.event.type.EventType
import com.stark.shoot.domain.user.vo.UserId
import com.stark.shoot.infrastructure.exception.web.KafkaPublishException
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.*
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.SendResult
import java.time.Instant
import java.util.concurrent.CompletableFuture

@DisplayName("Kafka 메시지 발행 어댑터 테스트")
class KafkaMessagePublishAdapterTest {

    private val kafkaTemplate = mock(KafkaTemplate::class.java) as KafkaTemplate<String, MessageEvent>
    private val adapter = PublishKafkaAdapter(kafkaTemplate)

    private fun createChatMessage(): ChatMessage {
        return ChatMessage(
            id = MessageId.from("test-message-id"),
            roomId = ChatRoomId.from(1L),
            senderId = UserId.from(2L),
            content = MessageContent("Test message", MessageType.TEXT),
            status = MessageStatus.SENT,
            createdAt = Instant.now()
        )
    }

    private fun createMessageEvent(): MessageEvent {
        return MessageEvent(
            type = EventType.MESSAGE_CREATED,
            data = createChatMessage()
        )
    }

    @Test
    @DisplayName("[happy] 채팅 이벤트를 Kafka로 성공적으로 발행할 수 있다")
    fun `채팅 이벤트를 Kafka로 성공적으로 발행할 수 있다`() {
        // given
        val topic = "test-topic"
        val key = "test-key"
        val event = createMessageEvent()

        val future = CompletableFuture.completedFuture(mock(SendResult::class.java) as SendResult<String, MessageEvent>)
        `when`(kafkaTemplate.send(topic, key, event)).thenReturn(future)

        // when
        val result = adapter.publishChatEvent(topic, key, event)

        // then
        verify(kafkaTemplate).send(topic, key, event)
        assert(result.isDone)
        assert(!result.isCompletedExceptionally)
    }

    @Test
    @DisplayName("[bad] 채팅 이벤트 발행 실패 시 예외가 발생한다")
    fun `채팅 이벤트 발행 실패 시 예외가 발생한다`() {
        // given
        val topic = "test-topic"
        val key = "test-key"
        val event = createMessageEvent()

        val future = CompletableFuture<SendResult<String, MessageEvent>>()
        future.completeExceptionally(RuntimeException("Kafka send failed"))
        `when`(kafkaTemplate.send(topic, key, event)).thenReturn(future)

        // when
        val result = adapter.publishChatEvent(topic, key, event)

        // then
        var exceptionThrown = false
        try {
            result.join()
        } catch (e: Exception) {
            exceptionThrown = true
        }

        assert(exceptionThrown) { "Expected an exception to be thrown" }
    }

    @Test
    @DisplayName("[happy] 코루틴 기반 채팅 이벤트를 Kafka로 성공적으로 발행할 수 있다")
    fun `코루틴 기반 채팅 이벤트를 Kafka로 성공적으로 발행할 수 있다`() {
        // given
        val topic = "test-topic"
        val key = "test-key"
        val event = createMessageEvent()

        val future = CompletableFuture.completedFuture(mock(SendResult::class.java) as SendResult<String, MessageEvent>)
        `when`(kafkaTemplate.send(topic, key, event)).thenReturn(future)

        // when & then - no exception should be thrown
        runBlocking {
            adapter.publishChatEventSuspend(topic, key, event)
        }

        // then
        verify(kafkaTemplate).send(topic, key, event)
    }

    @Test
    @DisplayName("[bad] 코루틴 기반 채팅 이벤트 발행 실패 시 예외가 발생한다")
    fun `코루틴 기반 채팅 이벤트 발행 실패 시 예외가 발생한다`() {
        // given
        val topic = "test-topic"
        val key = "test-key"
        val event = createMessageEvent()

        val future = CompletableFuture<SendResult<String, MessageEvent>>()
        future.completeExceptionally(RuntimeException("Kafka send failed"))
        `when`(kafkaTemplate.send(topic, key, event)).thenReturn(future)

        // when & then
        assertThrows<KafkaPublishException> { 
            runBlocking {
                adapter.publishChatEventSuspend(topic, key, event)
            }
        }
    }
}
