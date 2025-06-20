package com.stark.shoot.domain.notification

import java.time.Instant
import com.stark.shoot.domain.common.vo.UserId

/**
 * 사용자별 알림 설정을 관리하는 애그리게이트
 */
data class NotificationSettings(
    val userId: UserId,
    val preferences: Map<NotificationType, Boolean> = emptyMap(),
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant? = null,
) {
    /** 특정 알림 타입이 활성화되어 있는지 확인한다 */
    fun isEnabled(type: NotificationType): Boolean {
        return preferences[type] ?: true
    }

    /** 알림 타입의 활성화 여부를 변경한다 */
    fun updatePreference(type: NotificationType, enabled: Boolean): NotificationSettings {
        val updated = preferences.toMutableMap()
        updated[type] = enabled
        return copy(preferences = updated, updatedAt = Instant.now())
    }

    /** 여러 알림 타입의 설정을 한번에 업데이트한다 */
    fun updateAll(prefs: Map<NotificationType, Boolean>): NotificationSettings {
        val updated = preferences.toMutableMap()
        updated.putAll(prefs)
        return copy(preferences = updated, updatedAt = Instant.now())
    }
}
