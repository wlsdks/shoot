package com.stark.shoot.adapter.`in`.rest.notification

import com.stark.shoot.application.port.`in`.notification.NotificationManagementUseCase
import com.stark.shoot.application.port.`in`.notification.command.DeleteAllNotificationsCommand
import com.stark.shoot.application.port.`in`.notification.command.DeleteNotificationCommand
import com.stark.shoot.domain.notification.vo.NotificationId
import com.stark.shoot.domain.user.vo.UserId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import org.springframework.http.HttpStatus

@DisplayName("NotificationDeleteController 단위 테스트")
class NotificationDeleteControllerTest {

    private val notificationManagementUseCase = mock(NotificationManagementUseCase::class.java)
    private val controller = NotificationDeleteController(notificationManagementUseCase)

    @Test
    @DisplayName("[happy] 특정 알림을 삭제한다")
    fun `특정 알림을 삭제한다`() {
        // given
        val notificationId = "notification123"
        val userId = 1L

        val command = DeleteNotificationCommand(
            notificationId = NotificationId.from(notificationId),
            userId = UserId.from(userId)
        )
        
        `when`(notificationManagementUseCase.deleteNotification(command)).thenReturn(true)

        // when
        val response = controller.deleteNotification(notificationId, userId)

        // then
        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body).isTrue()

        verify(notificationManagementUseCase).deleteNotification(command)
    }

    @Test
    @DisplayName("[happy] 존재하지 않는 알림 삭제 시 false를 반환한다")
    fun `존재하지 않는 알림 삭제 시 false를 반환한다`() {
        // given
        val notificationId = "nonexistent123"
        val userId = 1L

        val command = DeleteNotificationCommand(
            notificationId = NotificationId.from(notificationId),
            userId = UserId.from(userId)
        )
        
        `when`(notificationManagementUseCase.deleteNotification(command)).thenReturn(false)

        // when
        val response = controller.deleteNotification(notificationId, userId)

        // then
        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body).isFalse()

        verify(notificationManagementUseCase).deleteNotification(command)
    }

    @Test
    @DisplayName("[happy] 사용자의 모든 알림을 삭제한다")
    fun `사용자의 모든 알림을 삭제한다`() {
        // given
        val userId = 1L
        val deletedCount = 5

        val command = DeleteAllNotificationsCommand(
            userId = UserId.from(userId)
        )
        
        `when`(notificationManagementUseCase.deleteAllNotifications(command)).thenReturn(deletedCount)

        // when
        val response = controller.deleteAllNotifications(userId)

        // then
        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body).isEqualTo(deletedCount)

        verify(notificationManagementUseCase).deleteAllNotifications(command)
    }

    @Test
    @DisplayName("[happy] 알림이 없는 사용자의 모든 알림 삭제 시 0을 반환한다")
    fun `알림이 없는 사용자의 모든 알림 삭제 시 0을 반환한다`() {
        // given
        val userId = 1L

        val command = DeleteAllNotificationsCommand(
            userId = UserId.from(userId)
        )
        
        `when`(notificationManagementUseCase.deleteAllNotifications(command)).thenReturn(0)

        // when
        val response = controller.deleteAllNotifications(userId)

        // then
        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body).isEqualTo(0)

        verify(notificationManagementUseCase).deleteAllNotifications(command)
    }
}