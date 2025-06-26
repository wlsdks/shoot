package com.stark.shoot.adapter.`in`.web.notification

import com.stark.shoot.adapter.`in`.web.dto.notification.NotificationResponse
import com.stark.shoot.application.port.`in`.notification.NotificationManagementUseCase
import com.stark.shoot.domain.notification.Notification
import com.stark.shoot.domain.notification.type.NotificationType
import com.stark.shoot.domain.notification.type.SourceType
import com.stark.shoot.domain.notification.vo.NotificationId
import com.stark.shoot.domain.notification.vo.NotificationMessage
import com.stark.shoot.domain.notification.vo.NotificationTitle
import com.stark.shoot.domain.user.vo.UserId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import org.springframework.http.HttpStatus
import java.time.Instant

@DisplayName("NotificationReadController 단위 테스트")
class NotificationReadControllerTest {

    private val notificationManagementUseCase = mock(NotificationManagementUseCase::class.java)
    private val controller = NotificationReadController(notificationManagementUseCase)

    @Test
    @DisplayName("[happy] 특정 알림을 읽음 처리한다")
    fun `특정 알림을 읽음 처리한다`() {
        // given
        val notificationId = "notification123"
        val userId = 1L
        val now = Instant.now()
        
        val readNotification = createNotification(
            id = notificationId,
            userId = userId,
            title = "읽은 알림",
            message = "읽은 알림 내용",
            type = NotificationType.NEW_MESSAGE,
            sourceType = SourceType.CHAT,
            isRead = true,
            readAt = now
        )
        
        `when`(notificationManagementUseCase.markAsRead(
            NotificationId.from(notificationId),
            UserId.from(userId)
        )).thenReturn(readNotification)

        // when
        val response = controller.markAsRead(notificationId, userId)

        // then
        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body?.id).isEqualTo(notificationId)
        assertThat(response.body?.isRead).isTrue()
        assertThat(response.body?.readAt).isEqualTo(now)

        verify(notificationManagementUseCase).markAsRead(
            NotificationId.from(notificationId),
            UserId.from(userId)
        )
    }

    @Test
    @DisplayName("[happy] 사용자의 모든 알림을 읽음 처리한다")
    fun `사용자의 모든 알림을 읽음 처리한다`() {
        // given
        val userId = 1L
        val markedCount = 5

        `when`(notificationManagementUseCase.markAllAsRead(
            UserId.from(userId)
        )).thenReturn(markedCount)

        // when
        val response = controller.markAllAsRead(userId)

        // then
        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body).isEqualTo(markedCount)

        verify(notificationManagementUseCase).markAllAsRead(
            UserId.from(userId)
        )
    }

    @Test
    @DisplayName("[happy] 사용자의 알림을 타입별로 읽음 처리한다")
    fun `사용자의 알림을 타입별로 읽음 처리한다`() {
        // given
        val userId = 1L
        val type = "FRIEND_REQUEST"
        val markedCount = 3

        `when`(notificationManagementUseCase.markAllAsReadByType(
            UserId.from(userId),
            NotificationType.valueOf(type)
        )).thenReturn(markedCount)

        // when
        val response = controller.markAllAsReadByType(userId, type)

        // then
        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body).isEqualTo(markedCount)

        verify(notificationManagementUseCase).markAllAsReadByType(
            UserId.from(userId),
            NotificationType.valueOf(type)
        )
    }

    @Test
    @DisplayName("[happy] 사용자의 알림을 소스별로 읽음 처리한다")
    fun `사용자의 알림을 소스별로 읽음 처리한다`() {
        // given
        val userId = 1L
        val sourceType = "CHAT"
        val sourceId = "chat123"
        val markedCount = 2

        `when`(notificationManagementUseCase.markAllAsReadBySource(
            UserId.from(userId),
            SourceType.valueOf(sourceType),
            sourceId
        )).thenReturn(markedCount)

        // when
        val response = controller.markAllAsReadBySource(userId, sourceType, sourceId)

        // then
        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body).isEqualTo(markedCount)

        verify(notificationManagementUseCase).markAllAsReadBySource(
            UserId.from(userId),
            SourceType.valueOf(sourceType),
            sourceId
        )
    }

    @Test
    @DisplayName("[happy] 사용자의 알림을 소스 타입별로 읽음 처리한다 (sourceId 없음)")
    fun `사용자의 알림을 소스 타입별로 읽음 처리한다 (sourceId 없음)`() {
        // given
        val userId = 1L
        val sourceType = "CHAT"
        val markedCount = 4

        `when`(notificationManagementUseCase.markAllAsReadBySource(
            UserId.from(userId),
            SourceType.valueOf(sourceType),
            null
        )).thenReturn(markedCount)

        // when
        val response = controller.markAllAsReadBySource(userId, sourceType, null)

        // then
        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body).isEqualTo(markedCount)

        verify(notificationManagementUseCase).markAllAsReadBySource(
            UserId.from(userId),
            SourceType.valueOf(sourceType),
            null
        )
    }

    // 테스트용 Notification 객체 생성 헬퍼 메서드
    private fun createNotification(
        id: String,
        userId: Long,
        title: String,
        message: String,
        type: NotificationType,
        sourceType: SourceType,
        sourceId: String = "source123",
        isRead: Boolean = false,
        readAt: Instant? = null,
        metadata: Map<String, Any> = emptyMap()
    ): Notification {
        return Notification(
            id = NotificationId.from(id),
            userId = UserId.from(userId),
            title = NotificationTitle.from(title),
            message = NotificationMessage.from(message),
            type = type,
            sourceId = sourceId,
            sourceType = sourceType,
            isRead = isRead,
            readAt = readAt,
            metadata = metadata
        )
    }
}