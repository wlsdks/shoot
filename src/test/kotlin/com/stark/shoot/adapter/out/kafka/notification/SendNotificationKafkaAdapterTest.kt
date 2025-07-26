package com.stark.shoot.adapter.out.kafka.notification

import com.fasterxml.jackson.databind.ObjectMapper
import com.stark.shoot.adapter.out.kafka.PublishNotificationKafkaAdapter
import com.stark.shoot.domain.notification.Notification
import com.stark.shoot.domain.notification.type.NotificationType
import com.stark.shoot.domain.notification.type.SourceType
import com.stark.shoot.domain.notification.vo.NotificationId
import com.stark.shoot.domain.notification.vo.NotificationMessage
import com.stark.shoot.domain.notification.vo.NotificationTitle
import com.stark.shoot.domain.user.vo.UserId
import com.stark.shoot.infrastructure.exception.web.RedisOperationException
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.*
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.SendResult
import java.time.Instant
import java.util.concurrent.CompletableFuture

@DisplayName("Kafka 알림 전송 어댑터 테스트")
class SendNotificationKafkaAdapterTest {

    private val kafkaTemplate = mock(KafkaTemplate::class.java) as KafkaTemplate<String, String>
    private val objectMapper = mock(ObjectMapper::class.java)
    private val adapter = PublishNotificationKafkaAdapter(kafkaTemplate, objectMapper)

    private fun createNotification(userId: Long = 1L): Notification {
        return Notification(
            id = NotificationId.from("test-notification-id"),
            userId = UserId.from(userId),
            title = NotificationTitle.from("Test Notification Title"),
            message = NotificationMessage.from("Test notification message"),
            type = NotificationType.NEW_MESSAGE,
            sourceId = "test-source-id",
            sourceType = SourceType.CHAT_ROOM,
            createdAt = Instant.now()
        )
    }

    @Test
    @DisplayName("[happy] 단일 알림을 Kafka로 성공적으로 전송할 수 있다")
    fun `단일 알림을 Kafka로 성공적으로 전송할 수 있다`() {
        // given
        val notification = createNotification()
        val notificationJson = "{\"id\":\"test-notification-id\",\"type\":\"NEW_MESSAGE\"}"

        `when`(objectMapper.writeValueAsString(notification)).thenReturn(notificationJson)

        val future = CompletableFuture.completedFuture(mock(SendResult::class.java) as SendResult<String, String>)
        `when`(kafkaTemplate.send("user-notifications", notification.userId.value.toString(), notificationJson)).thenReturn(future)

        // when
        adapter.sendNotification(notification)

        // then
        verify(objectMapper).writeValueAsString(notification)
        verify(kafkaTemplate).send("user-notifications", notification.userId.value.toString(), notificationJson)
    }

    @Test
    @DisplayName("[happy] JSON 변환 실패 시 예외가 발생한다")
    fun `JSON 변환 실패 시 예외가 발생한다`() {
        // given
        val notification = createNotification()

        `when`(objectMapper.writeValueAsString(notification)).thenThrow(RuntimeException("JSON serialization failed"))

        // when & then
        assertThrows<RedisOperationException> { 
            adapter.sendNotification(notification)
        }
    }

    @Test
    @DisplayName("[happy] Kafka 전송 실패 시 예외가 발생한다")
    fun `Kafka 전송 실패 시 예외가 발생한다`() {
        // given
        val notification = createNotification()
        val notificationJson = "{\"id\":\"test-notification-id\",\"type\":\"NEW_MESSAGE\"}"

        `when`(objectMapper.writeValueAsString(notification)).thenReturn(notificationJson)

        // Make the kafkaTemplate.send method throw an exception directly
        `when`(kafkaTemplate.send("user-notifications", notification.userId.value.toString(), notificationJson))
            .thenThrow(RuntimeException("Kafka send failed"))

        // when & then
        assertThrows<RedisOperationException> { 
            adapter.sendNotification(notification)
        }
    }

    @Test
    @DisplayName("[happy] 여러 알림을 Kafka로 성공적으로 전송할 수 있다")
    fun `여러 알림을 Kafka로 성공적으로 전송할 수 있다`() {
        // given
        val notifications = listOf(
            createNotification(1L),
            createNotification(2L),
            createNotification(3L)
        )

        for (notification in notifications) {
            val notificationJson = "{\"id\":\"test-notification-id\",\"type\":\"NEW_MESSAGE\"}"
            `when`(objectMapper.writeValueAsString(notification)).thenReturn(notificationJson)

            val future = CompletableFuture.completedFuture(mock(SendResult::class.java) as SendResult<String, String>)
            `when`(kafkaTemplate.send("user-notifications", notification.userId.value.toString(), notificationJson)).thenReturn(future)
        }

        // when
        adapter.sendNotifications(notifications)

        // then
        for (notification in notifications) {
            verify(objectMapper).writeValueAsString(notification)
            verify(kafkaTemplate).send("user-notifications", notification.userId.value.toString(), 
                objectMapper.writeValueAsString(notification))
        }
    }

    @Test
    @DisplayName("[happy] 빈 알림 목록은 아무 작업도 수행하지 않는다")
    fun `빈 알림 목록은 아무 작업도 수행하지 않는다`() {
        // given
        val notifications = emptyList<Notification>()

        // when
        adapter.sendNotifications(notifications)

        // then
        verifyNoInteractions(objectMapper)
        verifyNoInteractions(kafkaTemplate)
    }
}
