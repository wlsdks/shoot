package com.stark.shoot.domain.notification

import com.stark.shoot.domain.notification.type.NotificationType
import com.stark.shoot.domain.user.vo.UserId
import java.time.Instant

/**
 * 사용자별 알림 설정을 관리하는 애그리게이트
 */
data class NotificationSettings(
    val userId: UserId,
    var preferences: Map<NotificationType, Boolean> = emptyMap(),
    var createdAt: Instant = Instant.now(),
    var updatedAt: Instant? = null,
) {
    /** 특정 알림 타입이 활성화되어 있는지 확인한다 */
    fun isEnabled(type: NotificationType): Boolean {
        return preferences[type] ?: true
    }

    /** 알림 타입의 활성화 여부를 변경한다 */
    fun updatePreference(type: NotificationType, enabled: Boolean) {
        val updated = preferences.toMutableMap()
        updated[type] = enabled
        preferences = updated
        updatedAt = Instant.now()
    }

    /** 여러 알림 타입의 설정을 한번에 업데이트한다 */
    fun updateAll(prefs: Map<NotificationType, Boolean>) {
        val updated = preferences.toMutableMap()
        updated.putAll(prefs)
        preferences = updated
        updatedAt = Instant.now()
    }
}
