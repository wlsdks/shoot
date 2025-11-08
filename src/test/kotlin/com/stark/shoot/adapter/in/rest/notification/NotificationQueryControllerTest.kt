package com.stark.shoot.adapter.`in`.rest.notification

import com.stark.shoot.application.port.`in`.notification.NotificationQueryUseCase
import com.stark.shoot.application.port.`in`.notification.command.*
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
import org.hamcrest.Matchers.hasSize

@DisplayName("NotificationQueryController 단위 테스트")
class NotificationQueryControllerTest {

    private val notificationQueryUseCase = mock(NotificationQueryUseCase::class.java)
    private val authentication = mock(Authentication::class.java)
    private val controller = NotificationQueryController(notificationQueryUseCase)

    @Test
    @DisplayName("[happy] 사용자의 알림 목록을 조회한다")
    fun `사용자의 알림 목록을 조회한다`() {
        // given
        val userId = 1L
        val limit = 20
        val offset = 0

        val notifications = listOf(
            createNotification(
                id = "notification1",
                userId = userId,
                title = "첫 번째 알림",
                message = "첫 번째 알림 내용",
                type = NotificationType.NEW_MESSAGE,
                sourceType = SourceType.CHAT
            ),
            createNotification(
                id = "notification2",
                userId = userId,
                title = "두 번째 알림",
                message = "두 번째 알림 내용",
                type = NotificationType.MENTION,
                sourceType = SourceType.CHAT_ROOM
            )
        )

        val command = GetNotificationsCommand(
            userId = UserId.from(userId),
            limit = limit,
            offset = offset
        )
        
        `when`(authentication.name).thenReturn(userId.toString())
        `when`(notificationQueryUseCase.getNotificationsForUser(command)).thenReturn(notifications)

        // when
        val response = controller.getNotifications(authentication, limit, offset)

        // then
        assertThat(response.success).isTrue()
        assertThat(response.data).hasSize(2)
        assertThat(response.data?.get(0)?.id).isEqualTo("notification1")
        assertThat(response.data?.get(0)?.title).isEqualTo("첫 번째 알림")
        assertThat(response.data?.get(0)?.type).isEqualTo(NotificationType.NEW_MESSAGE.name)
        assertThat(response.data?.get(1)?.id).isEqualTo("notification2")
        assertThat(response.data?.get(1)?.title).isEqualTo("두 번째 알림")
        assertThat(response.data?.get(1)?.type).isEqualTo(NotificationType.MENTION.name)

        verify(notificationQueryUseCase).getNotificationsForUser(
            GetNotificationsCommand(
                userId = UserId.from(userId),
                limit = limit,
                offset = offset
            )
        )
    }

    @Test
    @DisplayName("[happy] 사용자의 읽지 않은 알림 목록을 조회한다")
    fun `사용자의 읽지 않은 알림 목록을 조회한다`() {
        // given
        val userId = 1L
        val limit = 20
        val offset = 0

        val notifications = listOf(
            createNotification(
                id = "notification1",
                userId = userId,
                title = "읽지 않은 알림 1",
                message = "읽지 않은 알림 내용 1",
                type = NotificationType.NEW_MESSAGE,
                sourceType = SourceType.CHAT,
                isRead = false
            ),
            createNotification(
                id = "notification2",
                userId = userId,
                title = "읽지 않은 알림 2",
                message = "읽지 않은 알림 내용 2",
                type = NotificationType.FRIEND_REQUEST,
                sourceType = SourceType.FRIEND,
                isRead = false
            )
        )

        val command = GetUnreadNotificationsCommand(
            userId = UserId.from(userId),
            limit = limit,
            offset = offset
        )
        
        `when`(authentication.name).thenReturn(userId.toString())
        `when`(notificationQueryUseCase.getUnreadNotificationsForUser(command)).thenReturn(notifications)

        // when
        val response = controller.getUnreadNotifications(authentication, limit, offset)

        // then
        assertThat(response.success).isTrue()
        assertThat(response.data).hasSize(2)
        assertThat(response.data?.get(0)?.id).isEqualTo("notification1")
        assertThat(response.data?.get(0)?.isRead).isFalse()
        assertThat(response.data?.get(1)?.id).isEqualTo("notification2")
        assertThat(response.data?.get(1)?.isRead).isFalse()

        verify(notificationQueryUseCase).getUnreadNotificationsForUser(
            GetUnreadNotificationsCommand(
                userId = UserId.from(userId),
                limit = limit,
                offset = offset
            )
        )
    }

    @Test
    @DisplayName("[happy] 사용자의 읽지 않은 알림 개수를 조회한다")
    fun `사용자의 읽지 않은 알림 개수를 조회한다`() {
        // given
        val userId = 1L
        val unreadCount = 5

        val command = GetUnreadNotificationCountCommand(
            userId = UserId.from(userId)
        )
        `when`(authentication.name).thenReturn(userId.toString())
        `when`(notificationQueryUseCase.getUnreadNotificationCount(command)).thenReturn(unreadCount)

        // when
        val response = controller.getUnreadNotificationCount(authentication)

        // then
        assertThat(response.success).isTrue()
        assertThat(response.data).isEqualTo(unreadCount)

        verify(notificationQueryUseCase).getUnreadNotificationCount(
            GetUnreadNotificationCountCommand(
                userId = UserId.from(userId)
            )
        )
    }

    @Test
    @DisplayName("[happy] 사용자의 알림을 타입별로 조회한다")
    fun `사용자의 알림을 타입별로 조회한다`() {
        // given
        val userId = 1L
        val type = "FRIEND_REQUEST"
        val limit = 20
        val offset = 0

        val notifications = listOf(
            createNotification(
                id = "notification1",
                userId = userId,
                title = "친구 요청 알림 1",
                message = "친구 요청 알림 내용 1",
                type = NotificationType.FRIEND_REQUEST,
                sourceType = SourceType.FRIEND
            ),
            createNotification(
                id = "notification2",
                userId = userId,
                title = "친구 요청 알림 2",
                message = "친구 요청 알림 내용 2",
                type = NotificationType.FRIEND_REQUEST,
                sourceType = SourceType.FRIEND
            )
        )

        val command = GetNotificationsByTypeCommand(
            userId = UserId.from(userId),
            type = NotificationType.valueOf(type),
            limit = limit,
            offset = offset
        )
        
        `when`(authentication.name).thenReturn(userId.toString())
        `when`(notificationQueryUseCase.getNotificationsByType(command)).thenReturn(notifications)

        // when
        val response = controller.getNotificationsByType(authentication, type, limit, offset)

        // then
        assertThat(response.success).isTrue()
        assertThat(response.data).hasSize(2)
        assertThat(response.data?.get(0)?.type).isEqualTo(NotificationType.FRIEND_REQUEST.name)
        assertThat(response.data?.get(1)?.type).isEqualTo(NotificationType.FRIEND_REQUEST.name)

        verify(notificationQueryUseCase).getNotificationsByType(
            GetNotificationsByTypeCommand(
                userId = UserId.from(userId),
                type = NotificationType.valueOf(type),
                limit = limit,
                offset = offset
            )
        )
    }

    @Test
    @DisplayName("[happy] 사용자의 알림을 소스별로 조회한다")
    fun `사용자의 알림을 소스별로 조회한다`() {
        // given
        val userId = 1L
        val sourceType = "CHAT"
        val sourceId = "chat123"
        val limit = 20
        val offset = 0

        val notifications = listOf(
            createNotification(
                id = "notification1",
                userId = userId,
                title = "채팅 알림 1",
                message = "채팅 알림 내용 1",
                type = NotificationType.NEW_MESSAGE,
                sourceType = SourceType.CHAT,
                sourceId = sourceId
            ),
            createNotification(
                id = "notification2",
                userId = userId,
                title = "채팅 알림 2",
                message = "채팅 알림 내용 2",
                type = NotificationType.MENTION,
                sourceType = SourceType.CHAT,
                sourceId = sourceId
            )
        )

        val command = GetNotificationsBySourceCommand(
            userId = UserId.from(userId),
            sourceType = SourceType.valueOf(sourceType),
            sourceId = sourceId,
            limit = limit,
            offset = offset
        )
        
        `when`(authentication.name).thenReturn(userId.toString())
        `when`(notificationQueryUseCase.getNotificationsBySource(command)).thenReturn(notifications)

        // when
        val response = controller.getNotificationsBySource(authentication, sourceType, sourceId, limit, offset)

        // then
        assertThat(response.success).isTrue()
        assertThat(response.data).hasSize(2)
        assertThat(response.data?.get(0)?.sourceType).isEqualTo(SourceType.CHAT.name)
        assertThat(response.data?.get(0)?.sourceId).isEqualTo(sourceId)
        assertThat(response.data?.get(1)?.sourceType).isEqualTo(SourceType.CHAT.name)
        assertThat(response.data?.get(1)?.sourceId).isEqualTo(sourceId)

        verify(notificationQueryUseCase).getNotificationsBySource(
            GetNotificationsBySourceCommand(
                userId = UserId.from(userId),
                sourceType = SourceType.valueOf(sourceType),
                sourceId = sourceId,
                limit = limit,
                offset = offset
            )
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
