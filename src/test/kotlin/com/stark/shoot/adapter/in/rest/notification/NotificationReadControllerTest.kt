package com.stark.shoot.adapter.`in`.rest.notification

import com.stark.shoot.application.port.`in`.notification.NotificationManagementUseCase
import com.stark.shoot.application.port.`in`.notification.command.MarkAllNotificationsAsReadCommand
import com.stark.shoot.application.port.`in`.notification.command.MarkAllNotificationsBySourceAsReadCommand
import com.stark.shoot.application.port.`in`.notification.command.MarkAllNotificationsByTypeAsReadCommand
import com.stark.shoot.application.port.`in`.notification.command.MarkNotificationAsReadCommand
import com.stark.shoot.domain.notification.Notification
import com.stark.shoot.domain.notification.type.NotificationType
import com.stark.shoot.domain.notification.type.SourceType
import com.stark.shoot.domain.notification.vo.NotificationId
import com.stark.shoot.domain.notification.vo.NotificationMessage
import com.stark.shoot.domain.notification.vo.NotificationTitle
import com.stark.shoot.domain.shared.UserId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import org.springframework.http.HttpStatus
import org.springframework.security.core.Authentication
import java.time.Instant

@DisplayName("NotificationReadController 단위 테스트")
class NotificationReadControllerTest {

    private val notificationManagementUseCase = mock(NotificationManagementUseCase::class.java)
    private val authentication = mock(Authentication::class.java)
    private val controller = NotificationReadController(notificationManagementUseCase)

    @Test
    @DisplayName("[happy] 특정 알림을 읽음 처리한다")
    fun `특정 알림을 읽음 처리한다`() {
        // given
        val notificationId = "notification123"
        val userId = 1L
        val now = Instant.now()
        
        val command = MarkNotificationAsReadCommand(
            notificationId = NotificationId.from(notificationId),
            userId = UserId.from(userId)
        )
        
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
        
        `when`(authentication.name).thenReturn(userId.toString())
        `when`(notificationManagementUseCase.markAsRead(command)).thenReturn(readNotification)

        // when
        val response = controller.markAsRead(authentication, notificationId)

        // then
        assertThat(response.success).isTrue()
        assertThat(response.data?.id).isEqualTo(notificationId)
        assertThat(response.data?.isRead).isTrue()
        assertThat(response.data?.readAt).isEqualTo(now)

        verify(notificationManagementUseCase).markAsRead(command)
    }

    @Test
    @DisplayName("[happy] 사용자의 모든 알림을 읽음 처리한다")
    fun `사용자의 모든 알림을 읽음 처리한다`() {
        // given
        val userId = 1L
        val markedCount = 5

        val command = MarkAllNotificationsAsReadCommand(
            userId = UserId.from(userId)
        )
        
        `when`(authentication.name).thenReturn(userId.toString())
        `when`(notificationManagementUseCase.markAllAsRead(command)).thenReturn(markedCount)

        // when
        val response = controller.markAllAsRead(authentication)

        // then
        assertThat(response.success).isTrue()
        assertThat(response.data).isEqualTo(markedCount)

        verify(notificationManagementUseCase).markAllAsRead(command)
    }

    @Test
    @DisplayName("[happy] 사용자의 알림을 타입별로 읽음 처리한다")
    fun `사용자의 알림을 타입별로 읽음 처리한다`() {
        // given
        val userId = 1L
        val type = "FRIEND_REQUEST"
        val markedCount = 3

        val command = MarkAllNotificationsByTypeAsReadCommand(
            userId = UserId.from(userId),
            type = NotificationType.valueOf(type)
        )
        
        `when`(authentication.name).thenReturn(userId.toString())
        `when`(notificationManagementUseCase.markAllAsReadByType(command)).thenReturn(markedCount)

        // when
        val response = controller.markAllAsReadByType(authentication, type)

        // then
        assertThat(response.success).isTrue()
        assertThat(response.data).isEqualTo(markedCount)

        verify(notificationManagementUseCase).markAllAsReadByType(command)
    }

    @Test
    @DisplayName("[happy] 사용자의 알림을 소스별로 읽음 처리한다")
    fun `사용자의 알림을 소스별로 읽음 처리한다`() {
        // given
        val userId = 1L
        val sourceType = "CHAT"
        val sourceId = "chat123"
        val markedCount = 2

        val command = MarkAllNotificationsBySourceAsReadCommand(
            userId = UserId.from(userId),
            sourceType = SourceType.valueOf(sourceType),
            sourceId = sourceId
        )
        
        `when`(authentication.name).thenReturn(userId.toString())
        `when`(notificationManagementUseCase.markAllAsReadBySource(command)).thenReturn(markedCount)

        // when
        val response = controller.markAllAsReadBySource(authentication, sourceType, sourceId)

        // then
        assertThat(response.success).isTrue()
        assertThat(response.data).isEqualTo(markedCount)

        verify(notificationManagementUseCase).markAllAsReadBySource(command)
    }

    @Test
    @DisplayName("[happy] 사용자의 알림을 소스 타입별로 읽음 처리한다 (sourceId 없음)")
    fun `사용자의 알림을 소스 타입별로 읽음 처리한다 (sourceId 없음)`() {
        // given
        val userId = 1L
        val sourceType = "CHAT"
        val markedCount = 4

        val command = MarkAllNotificationsBySourceAsReadCommand(
            userId = UserId.from(userId),
            sourceType = SourceType.valueOf(sourceType),
            sourceId = null
        )
        
        `when`(authentication.name).thenReturn(userId.toString())
        `when`(notificationManagementUseCase.markAllAsReadBySource(command)).thenReturn(markedCount)

        // when
        val response = controller.markAllAsReadBySource(authentication, sourceType, null)

        // then
        assertThat(response.success).isTrue()
        assertThat(response.data).isEqualTo(markedCount)

        verify(notificationManagementUseCase).markAllAsReadBySource(command)
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