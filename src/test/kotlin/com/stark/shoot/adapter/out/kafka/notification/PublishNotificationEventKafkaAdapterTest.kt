package com.stark.shoot.adapter.out.kafka.notification

import com.fasterxml.jackson.databind.ObjectMapper
import com.stark.shoot.domain.chatroom.vo.ChatRoomId
import com.stark.shoot.domain.event.NewMessageEvent
import com.stark.shoot.domain.notification.type.NotificationType
import com.stark.shoot.domain.notification.type.SourceType
import com.stark.shoot.domain.user.vo.UserId
import com.stark.shoot.infrastructure.exception.web.KafkaPublishException
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.*
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.SendResult
import java.time.Instant
import java.util.concurrent.CompletableFuture

@DisplayName("Kafka 알림 이벤트 발행 어댑터 테스트")
class PublishNotificationEventKafkaAdapterTest {

    private val kafkaTemplate = mock(KafkaTemplate::class.java) as KafkaTemplate<String, String>
    private val objectMapper = mock(ObjectMapper::class.java)
    private val adapter = PublishNotificationEventKafkaAdapter(kafkaTemplate, objectMapper)

    private fun createNotificationEvent(): NewMessageEvent {
        return NewMessageEvent(
            id = "test-notification-id",
            roomId = ChatRoomId.from(1L),
            senderId = UserId.from(2L),
            senderName = "Test User",
            messageContent = "Test message content",
            recipientIds = setOf(UserId.from(3L), UserId.from(4L)),
            timestamp = Instant.now()
        )
    }

    @Test
    @DisplayName("[happy] 알림 이벤트를 Kafka로 성공적으로 발행할 수 있다")
    fun `알림 이벤트를 Kafka로 성공적으로 발행할 수 있다`() {
        // given
        val event = createNotificationEvent()
        val eventJson = "{\"id\":\"test-notification-id\",\"type\":\"NEW_MESSAGE\"}"

        `when`(objectMapper.writeValueAsString(event)).thenReturn(eventJson)

        val future = CompletableFuture.completedFuture(mock(SendResult::class.java) as SendResult<String, String>)
        `when`(kafkaTemplate.send("notification-events", event.sourceId, eventJson)).thenReturn(future)

        // when
        adapter.publishEvent(event)

        // then
        verify(objectMapper).writeValueAsString(event)
        verify(kafkaTemplate).send("notification-events", event.sourceId, eventJson)
    }

    @Test
    @DisplayName("[happy] JSON 변환 실패 시 예외가 발생한다")
    fun `JSON 변환 실패 시 예외가 발생한다`() {
        // given
        val event = createNotificationEvent()

        `when`(objectMapper.writeValueAsString(event)).thenThrow(RuntimeException("JSON serialization failed"))

        // when & then
        assertThrows<KafkaPublishException> { 
            adapter.publishEvent(event)
        }
    }

    @Test
    @DisplayName("[happy] Kafka 발행 실패 시 예외가 발생한다")
    fun `Kafka 발행 실패 시 예외가 발생한다`() {
        // given
        val event = createNotificationEvent()
        val eventJson = "{\"id\":\"test-notification-id\",\"type\":\"NEW_MESSAGE\"}"

        `when`(objectMapper.writeValueAsString(event)).thenReturn(eventJson)

        // Instead of returning a CompletableFuture that completes exceptionally,
        // make the kafkaTemplate.send method throw an exception directly
        `when`(kafkaTemplate.send("notification-events", event.sourceId, eventJson))
            .thenThrow(RuntimeException("Kafka send failed"))

        // when & then
        assertThrows<KafkaPublishException> { 
            adapter.publishEvent(event)
        }
    }
}
