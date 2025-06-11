package com.stark.shoot.domain.notification

import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class NotificationTest {
    @Test
    fun markReadAndDelete() {
        val n = Notification.create(
            userId = 1L,
            title = "t",
            message = "m",
            type = NotificationType.NEW_MESSAGE,
            sourceId = "s",
            sourceType = SourceType.CHAT
        )
        val read = n.markAsRead()
        assertTrue(read.isRead)
        val deleted = read.markAsDeleted()
        assertTrue(deleted.isDeleted)
    }

    @Test
    fun validateOwnershipThrows() {
        val n = Notification.create(
            userId = 1L,
            title = "t",
            message = "m",
            type = NotificationType.NEW_MESSAGE,
            sourceId = "s",
            sourceType = SourceType.CHAT
        )
        assertFailsWith<NotificationException> { n.validateOwnership(2L) }
    }
}
