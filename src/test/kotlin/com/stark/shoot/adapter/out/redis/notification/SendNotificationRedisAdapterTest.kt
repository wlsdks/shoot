package com.stark.shoot.adapter.out.redis.notification

import com.fasterxml.jackson.databind.ObjectMapper
import com.stark.shoot.domain.notification.Notification
import com.stark.shoot.domain.notification.type.NotificationType
import com.stark.shoot.domain.notification.type.SourceType
import com.stark.shoot.domain.notification.vo.NotificationId
import com.stark.shoot.domain.notification.vo.NotificationMessage
import com.stark.shoot.domain.notification.vo.NotificationTitle
import com.stark.shoot.domain.user.vo.UserId
import com.stark.shoot.domain.exception.web.RedisOperationException
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import org.springframework.data.redis.core.StringRedisTemplate
import java.time.Instant

@DisplayName("SendNotificationRedisAdapter 테스트")
class SendNotificationRedisAdapterTest {

    private val redisTemplate = mock(StringRedisTemplate::class.java)
    private val objectMapper = mock(ObjectMapper::class.java)
    private val adapter = SendNotificationRedisAdapter(redisTemplate, objectMapper)

    private fun createNotification(userId: Long = 1L): Notification {
        return Notification(
            id = NotificationId.from("id"),
            userId = UserId.from(userId),
            title = NotificationTitle.from("t"),
            message = NotificationMessage.from("m"),
            type = NotificationType.NEW_MESSAGE,
            sourceId = "s",
            sourceType = SourceType.CHAT_ROOM,
            createdAt = Instant.now()
        )
    }

    @Test
    @DisplayName("[happy] 단일 알림을 Redis로 전송할 수 있다")
    fun `단일 알림을 Redis로 전송할 수 있다`() {
        val notification = createNotification()
        val json = "{}"
        `when`(objectMapper.writeValueAsString(notification)).thenReturn(json)
        `when`(redisTemplate.convertAndSend("notification:user:1", json)).thenReturn(1L)

        adapter.sendNotification(notification)

        verify(redisTemplate).convertAndSend("notification:user:1", json)
        verify(objectMapper).writeValueAsString(notification)
    }

    @Test
    @DisplayName("[bad] 알림 전송 실패 시 예외가 발생한다")
    fun `알림 전송 실패 시 예외가 발생한다`() {
        val notification = createNotification()
        `when`(objectMapper.writeValueAsString(notification)).thenThrow(RuntimeException("fail"))

        assertThatThrownBy { adapter.sendNotification(notification) }
            .isInstanceOf(RedisOperationException::class.java)
    }

    @Test
    @DisplayName("[happy] 여러 알림을 Redis로 전송할 수 있다")
    fun `여러 알림을 Redis로 전송할 수 있다`() {
        val n1 = createNotification(1L)
        val n2 = createNotification(2L)
        val list = listOf(n1, n2)
        `when`(objectMapper.writeValueAsString(n1)).thenReturn("n1")
        `when`(objectMapper.writeValueAsString(n2)).thenReturn("n2")

        adapter.sendNotifications(list)

        verify(redisTemplate).convertAndSend("notification:user:1", "n1")
        verify(redisTemplate).convertAndSend("notification:user:2", "n2")
    }

    @Test
    @DisplayName("[happy] 빈 알림 목록은 아무 작업도 하지 않는다")
    fun `빈 알림 목록은 아무 작업도 하지 않는다`() {
        adapter.sendNotifications(emptyList())
        verifyNoInteractions(redisTemplate, objectMapper)
    }
}
