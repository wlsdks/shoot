package com.stark.shoot.domain.notification.service

import com.stark.shoot.domain.event.NotificationEvent
import com.stark.shoot.domain.notification.Notification
import com.stark.shoot.domain.notification.type.NotificationType
import com.stark.shoot.domain.notification.type.SourceType
import com.stark.shoot.domain.user.vo.UserId
import org.junit.jupiter.api.DisplayName
import kotlin.test.Test
import kotlin.test.assertEquals

@DisplayName("알림 도메인 서비스 테스트")
class NotificationDomainServiceTest {
    private val service = NotificationDomainService()

    @Test
    @DisplayName("[happy] 읽음 처리 후 삭제 상태로 변경할 수 있다")
    fun markAsReadAndDeleted() {
        val n = Notification.create(UserId.from(1L), "t", "m", NotificationType.NEW_MESSAGE, "s", SourceType.CHAT)
        val read = service.markNotificationsAsRead(listOf(n))
        val deleted = service.markNotificationsAsDeleted(read)
        assertEquals(true, deleted[0].isDeleted)
    }

    @Test
    @DisplayName("[happy] 읽지 않은 알림만 필터링한다")
    fun filterUnread() {
        val n1 = Notification.create(UserId.from(1L), "t", "m", NotificationType.NEW_MESSAGE, "s", SourceType.CHAT)
        val n2 = n1.markAsRead()
        val unread = service.filterUnread(listOf(n1, n2))
        assertEquals(1, unread.size)
    }

    @Test
    @DisplayName("[happy] 이벤트로부터 알림을 생성할 수 있다")
    fun createNotificationsFromEvent() {
        val event = object : NotificationEvent(
            type = NotificationType.NEW_MESSAGE,
            sourceId = "1",
            sourceType = SourceType.CHAT
        ) {
            override fun getRecipients(): Set<UserId> = setOf(UserId.from(1L), UserId.from(2L))
            override fun getTitle(): String = "title"
            override fun getMessage(): String = "msg"
        }

        val result = service.createNotificationsFromEvent(event)
        assertEquals(2, result.size)
        assertEquals(setOf(UserId.from(1L), UserId.from(2L)), result.map { it.userId }.toSet())
    }
}
