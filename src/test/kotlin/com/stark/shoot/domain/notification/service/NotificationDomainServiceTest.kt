package com.stark.shoot.domain.notification.service

import com.stark.shoot.domain.notification.Notification
import com.stark.shoot.domain.notification.NotificationType
import com.stark.shoot.domain.notification.SourceType
import com.stark.shoot.domain.notification.event.NotificationEvent
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

    @Test
    fun createNotificationsFromEvent() {
        val event = object : NotificationEvent(
            type = NotificationType.NEW_MESSAGE,
            sourceId = "1",
            sourceType = SourceType.CHAT
        ) {
            override fun getRecipients(): Set<Long> = setOf(1L, 2L)
            override fun getTitle(): String = "title"
            override fun getMessage(): String = "msg"
        }

        val result = service.createNotificationsFromEvent(event)
        assertEquals(2, result.size)
        assertEquals(setOf(1L, 2L), result.map { it.userId }.toSet())
    }
}
