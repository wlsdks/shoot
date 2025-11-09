package com.stark.shoot.domain.notification

import com.stark.shoot.domain.notification.type.NotificationType
import com.stark.shoot.domain.shared.UserId
import com.stark.shoot.infrastructure.annotation.AggregateRoot
import java.time.Instant

/**
 * 사용자별 알림 설정을 관리하는 애그리게이트
 */
@AggregateRoot
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

    companion object {
        /**
         * 기본 알림 설정 생성
         * 모든 알림 타입이 활성화된 상태로 생성됨
         *
         * @param userId 사용자 ID
         * @return 기본 알림 설정
         */
        fun createDefault(userId: UserId): NotificationSettings {
            return NotificationSettings(
                userId = userId,
                preferences = emptyMap() // 기본값: 모든 알림 활성화
            )
        }

        /**
         * 사용자 지정 알림 설정 생성
         *
         * @param userId 사용자 ID
         * @param preferences 알림 타입별 활성화 여부
         * @return 알림 설정
         */
        fun create(
            userId: UserId,
            preferences: Map<NotificationType, Boolean>
        ): NotificationSettings {
            return NotificationSettings(
                userId = userId,
                preferences = preferences
            )
        }
    }
}
