package com.stark.shoot.domain.notification

import com.stark.shoot.domain.notification.type.NotificationType
import com.stark.shoot.domain.shared.UserId
import org.junit.jupiter.api.DisplayName
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@DisplayName("알림 설정 도메인 테스트")
class NotificationSettingsTest {

    @Test
    @DisplayName("[happy] 기본 설정이 모두 활성화되어 있다")
    fun defaultAllEnabled() {
        val settings = NotificationSettings(userId = UserId.from(1L))
        assertTrue(settings.isEnabled(NotificationType.NEW_MESSAGE))
    }

    @Test
    @DisplayName("[happy] 선호도 설정을 변경할 수 있다")
    fun updatePreference() {
        val settings = NotificationSettings(userId = UserId.from(1L))
        settings.updatePreference(NotificationType.NEW_MESSAGE, false)
        assertFalse(settings.isEnabled(NotificationType.NEW_MESSAGE))
    }

    @Test
    @DisplayName("[happy] 일괄 설정을 적용할 수 있다")
    fun updateAll() {
        val settings = NotificationSettings(userId = UserId.from(1L))
        settings.updateAll(mapOf(NotificationType.MENTION to false))
        assertFalse(settings.isEnabled(NotificationType.MENTION))
        assertTrue(settings.isEnabled(NotificationType.NEW_MESSAGE))
    }

}
