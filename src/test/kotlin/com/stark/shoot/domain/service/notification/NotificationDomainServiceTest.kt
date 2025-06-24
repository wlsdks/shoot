package com.stark.shoot.domain.service.notification

import com.stark.shoot.domain.event.NotificationEvent
import com.stark.shoot.domain.notification.Notification
import com.stark.shoot.domain.notification.service.NotificationDomainService
import com.stark.shoot.domain.notification.type.NotificationType
import com.stark.shoot.domain.notification.type.SourceType
import com.stark.shoot.domain.notification.vo.NotificationId
import com.stark.shoot.domain.notification.vo.NotificationMessage
import com.stark.shoot.domain.notification.vo.NotificationTitle
import com.stark.shoot.domain.user.vo.UserId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import java.time.Instant

@DisplayName("알림 도메인 서비스 테스트")
class NotificationDomainServiceTest {

    private val notificationDomainService = NotificationDomainService()

    private fun createNotification(
        id: String = "test-notification-id",
        userId: Long = 1L,
        isRead: Boolean = false,
        isDeleted: Boolean = false
    ): Notification {
        return Notification(
            id = NotificationId.from(id),
            userId = UserId.from(userId),
            title = NotificationTitle.from("Test Notification Title"),
            message = NotificationMessage.from("Test notification message"),
            type = NotificationType.NEW_MESSAGE,
            sourceId = "test-source-id",
            sourceType = SourceType.CHAT_ROOM,
            isRead = isRead,
            createdAt = Instant.now(),
            readAt = if (isRead) Instant.now() else null,
            isDeleted = isDeleted,
            deletedAt = if (isDeleted) Instant.now() else null
        )
    }

    @Nested
    @DisplayName("알림 읽음 처리 시")
    inner class MarkAsRead {

        @Test
        @DisplayName("[happy] 여러 알림을 읽음 처리할 수 있다")
        fun `여러 알림을 읽음 처리할 수 있다`() {
            // given
            val notifications = listOf(
                createNotification("1"),
                createNotification("2"),
                createNotification("3")
            )

            // when
            val result = notificationDomainService.markNotificationsAsRead(notifications)

            // then
            assertThat(result).hasSize(3)
            result.forEach { notification ->
                assertThat(notification.isRead).isTrue()
                assertThat(notification.readAt).isNotNull()
            }
        }

        @Test
        @DisplayName("[happy] 이미 읽은 알림은 변경되지 않는다")
        fun `이미 읽은 알림은 변경되지 않는다`() {
            // given
            val readNotification = createNotification(isRead = true)
            val readAt = readNotification.readAt

            // when
            val result = notificationDomainService.markNotificationsAsRead(listOf(readNotification))

            // then
            assertThat(result).hasSize(1)
            assertThat(result[0].isRead).isTrue()
            assertThat(result[0].readAt).isEqualTo(readAt)
        }
    }

    @Nested
    @DisplayName("알림 삭제 처리 시")
    inner class MarkAsDeleted {

        @Test
        @DisplayName("[happy] 여러 알림을 삭제 처리할 수 있다")
        fun `여러 알림을 삭제 처리할 수 있다`() {
            // given
            val notifications = listOf(
                createNotification("1"),
                createNotification("2"),
                createNotification("3")
            )

            // when
            val result = notificationDomainService.markNotificationsAsDeleted(notifications)

            // then
            assertThat(result).hasSize(3)
            result.forEach { notification ->
                assertThat(notification.isDeleted).isTrue()
                assertThat(notification.deletedAt).isNotNull()
            }
        }

        @Test
        @DisplayName("[happy] 이미 삭제된 알림은 변경되지 않는다")
        fun `이미 삭제된 알림은 변경되지 않는다`() {
            // given
            val deletedNotification = createNotification(isDeleted = true)
            val deletedAt = deletedNotification.deletedAt

            // when
            val result = notificationDomainService.markNotificationsAsDeleted(listOf(deletedNotification))

            // then
            assertThat(result).hasSize(1)
            assertThat(result[0].isDeleted).isTrue()
            assertThat(result[0].deletedAt).isEqualTo(deletedAt)
        }
    }

    @Nested
    @DisplayName("읽지 않은 알림 필터링 시")
    inner class FilterUnread {

        @Test
        @DisplayName("[happy] 읽지 않은 알림만 필터링할 수 있다")
        fun `읽지 않은 알림만 필터링할 수 있다`() {
            // given
            val notifications = listOf(
                createNotification("1", isRead = false),
                createNotification("2", isRead = true),
                createNotification("3", isRead = false)
            )

            // when
            val result = notificationDomainService.filterUnread(notifications)

            // then
            assertThat(result).hasSize(2)
            result.forEach { notification ->
                assertThat(notification.isRead).isFalse()
            }
        }

        @Test
        @DisplayName("[happy] 모든 알림이 읽은 상태면 빈 목록을 반환한다")
        fun `모든 알림이 읽은 상태면 빈 목록을 반환한다`() {
            // given
            val notifications = listOf(
                createNotification("1", isRead = true),
                createNotification("2", isRead = true)
            )

            // when
            val result = notificationDomainService.filterUnread(notifications)

            // then
            assertThat(result).isEmpty()
        }
    }

    @Nested
    @DisplayName("알림 이벤트로부터 알림 생성 시")
    inner class CreateFromEvent {

        @Test
        @DisplayName("[happy] 이벤트의 수신자별로 알림을 생성할 수 있다")
        fun `이벤트의 수신자별로 알림을 생성할 수 있다`() {
            // given
            val recipients = setOf(
                UserId.from(1L),
                UserId.from(2L),
                UserId.from(3L)
            )

            val event = mock(NotificationEvent::class.java)
            `when`(event.getRecipients()).thenReturn(recipients)
            `when`(event.getTitle()).thenReturn("Test Event Title")
            `when`(event.getMessage()).thenReturn("Test event message")
            `when`(event.type).thenReturn(NotificationType.NEW_MESSAGE)
            `when`(event.sourceId).thenReturn("test-source-id")
            `when`(event.sourceType).thenReturn(SourceType.CHAT_ROOM)
            `when`(event.metadata).thenReturn(emptyMap())

            // when
            val result = notificationDomainService.createNotificationsFromEvent(event)

            // then
            assertThat(result).hasSize(3)

            // Verify each notification has the correct recipient
            val recipientIds = result.map { it.userId }
            assertThat(recipientIds).containsExactlyInAnyOrderElementsOf(recipients)

            // Verify all notifications have the same event data
            result.forEach { notification ->
                assertThat(notification.title.value).isEqualTo("Test Event Title")
                assertThat(notification.message.value).isEqualTo("Test event message")
                assertThat(notification.type).isEqualTo(NotificationType.NEW_MESSAGE)
                assertThat(notification.sourceId).isEqualTo("test-source-id")
                assertThat(notification.sourceType).isEqualTo(SourceType.CHAT_ROOM)
            }
        }

        @Test
        @DisplayName("[happy] 수신자가 없으면 빈 목록을 반환한다")
        fun `수신자가 없으면 빈 목록을 반환한다`() {
            // given
            val event = mock(NotificationEvent::class.java)
            `when`(event.getRecipients()).thenReturn(emptySet())

            // when
            val result = notificationDomainService.createNotificationsFromEvent(event)

            // then
            assertThat(result).isEmpty()
        }
    }
}
