package com.stark.shoot.domain.notification.service

import com.stark.shoot.domain.notification.Notification
import com.stark.shoot.domain.notification.NotificationType
import com.stark.shoot.domain.notification.SourceType
import kotlin.test.Test
import kotlin.test.assertEquals

class NotificationDomainServiceTest {
    private val service = NotificationDomainService()

    @Test
    fun markAsReadAndDeleted() {
        val n = Notification.create(1L, "t", "m", NotificationType.NEW_MESSAGE, "s", SourceType.CHAT)
        val read = service.markNotificationsAsRead(listOf(n))
        val deleted = service.markNotificationsAsDeleted(read)
        assertEquals(true, deleted[0].isDeleted)
    }

    @Test
    fun filterUnread() {
        val n1 = Notification.create(1L, "t", "m", NotificationType.NEW_MESSAGE, "s", SourceType.CHAT)
        val n2 = n1.markAsRead()
        val unread = service.filterUnread(listOf(n1, n2))
        assertEquals(1, unread.size)
    }
}
