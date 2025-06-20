package com.stark.shoot.domain.notification

import com.stark.shoot.domain.exception.NotificationException
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import com.stark.shoot.domain.common.vo.UserId

class NotificationTest {
    @Test
    fun markReadAndDelete() {
        val n = Notification.create(
            userId = UserId.from(1L),
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
            userId = UserId.from(1L),
            title = "t",
            message = "m",
            type = NotificationType.NEW_MESSAGE,
            sourceId = "s",
            sourceType = SourceType.CHAT
        )
        assertFailsWith<NotificationException> { n.validateOwnership(UserId.from(2L)) }
    }
}
