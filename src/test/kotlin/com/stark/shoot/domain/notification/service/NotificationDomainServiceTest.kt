package com.stark.shoot.domain.notification.service

import com.stark.shoot.domain.notification.Notification
import com.stark.shoot.domain.notification.type.NotificationType
import com.stark.shoot.domain.notification.type.SourceType
import com.stark.shoot.domain.notification.event.NotificationEvent
import com.stark.shoot.domain.common.vo.UserId
import kotlin.test.Test
import kotlin.test.assertEquals

class NotificationDomainServiceTest {
    private val service = NotificationDomainService()

    @Test
    fun markAsReadAndDeleted() {
        val n = Notification.create(UserId.from(1L), "t", "m", NotificationType.NEW_MESSAGE, "s", SourceType.CHAT)
        val read = service.markNotificationsAsRead(listOf(n))
        val deleted = service.markNotificationsAsDeleted(read)
        assertEquals(true, deleted[0].isDeleted)
    }

    @Test
    fun filterUnread() {
        val n1 = Notification.create(UserId.from(1L), "t", "m", NotificationType.NEW_MESSAGE, "s", SourceType.CHAT)
        val n2 = n1.markAsRead()
        val unread = service.filterUnread(listOf(n1, n2))
        assertEquals(1, unread.size)
    }

    @Test
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
