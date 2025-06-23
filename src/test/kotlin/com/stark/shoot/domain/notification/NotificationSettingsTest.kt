package com.stark.shoot.domain.notification

import com.stark.shoot.domain.notification.type.NotificationType
import com.stark.shoot.domain.user.vo.UserId
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NotificationSettingsTest {

    @Test
    fun defaultAllEnabled() {
        val settings = NotificationSettings(userId = UserId.from(1L))
        assertTrue(settings.isEnabled(NotificationType.NEW_MESSAGE))
    }

    @Test
    fun updatePreference() {
        val settings = NotificationSettings(userId = UserId.from(1L))
            .updatePreference(NotificationType.NEW_MESSAGE, false)
        assertFalse(settings.isEnabled(NotificationType.NEW_MESSAGE))
    }

    @Test
    fun updateAll() {
        val settings = NotificationSettings(userId = UserId.from(1L))
            .updateAll(mapOf(NotificationType.MENTION to false))
        assertFalse(settings.isEnabled(NotificationType.MENTION))
        assertTrue(settings.isEnabled(NotificationType.NEW_MESSAGE))
    }

}
