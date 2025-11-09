package com.stark.shoot.domain.chatroom

import com.stark.shoot.domain.chatroom.vo.RetentionDays
import com.stark.shoot.infrastructure.annotation.ValueObject

@ValueObject
data class ChatRoomSettings(
    val isNotificationEnabled: Boolean = true,
    val retentionDays: RetentionDays? = null,
    val isEncrypted: Boolean = false,
    val customSettings: Map<String, Any> = emptyMap()
) {
    companion object {
        /**
         * 기본 채팅방 설정 생성
         *
         * @return 기본 설정이 적용된 ChatRoomSettings 객체
         */
        fun createDefault(): ChatRoomSettings {
            return ChatRoomSettings()
        }

        /**
         * 커스텀 채팅방 설정 생성
         *
         * @param isNotificationEnabled 알림 활성화 여부
         * @param retentionDays 메시지 보존 기간 (일)
         * @param isEncrypted 암호화 여부
         * @param customSettings 추가 커스텀 설정
         * @return 커스텀 설정이 적용된 ChatRoomSettings 객체
         */
        fun create(
            isNotificationEnabled: Boolean = true,
            retentionDays: RetentionDays? = null,
            isEncrypted: Boolean = false,
            customSettings: Map<String, Any> = emptyMap()
        ): ChatRoomSettings {
            return ChatRoomSettings(
                isNotificationEnabled = isNotificationEnabled,
                retentionDays = retentionDays,
                isEncrypted = isEncrypted,
                customSettings = customSettings
            )
        }
    }
    /**
     * 알림 설정 업데이트
     *
     * @param enabled 알림 활성화 여부
     * @return 업데이트된 ChatRoomSettings 객체
     */
    fun updateNotificationSettings(enabled: Boolean): ChatRoomSettings {
        return this.copy(isNotificationEnabled = enabled)
    }

    /**
     * 메시지 보존 기간 설정
     *
     * @param days 보존할 일수 (null이면 무기한 보존)
     * @return 업데이트된 ChatRoomSettings 객체
     */
    fun updateRetentionPolicy(days: RetentionDays?): ChatRoomSettings {
        return this.copy(retentionDays = days)
    }

    /**
     * 암호화 설정 업데이트
     *
     * @param encrypted 암호화 활성화 여부
     * @return 업데이트된 ChatRoomSettings 객체
     */
    fun updateEncryption(encrypted: Boolean): ChatRoomSettings {
        return this.copy(isEncrypted = encrypted)
    }

    /**
     * 커스텀 설정 추가
     *
     * @param key 설정 키
     * @param value 설정 값
     * @return 업데이트된 ChatRoomSettings 객체
     */
    fun addCustomSetting(key: String, value: Any): ChatRoomSettings {
        val updatedSettings = this.customSettings.toMutableMap()
        updatedSettings[key] = value
        return this.copy(customSettings = updatedSettings)
    }

    /**
     * 커스텀 설정 제거
     *
     * @param key 제거할 설정 키
     * @return 업데이트된 ChatRoomSettings 객체
     */
    fun removeCustomSetting(key: String): ChatRoomSettings {
        val updatedSettings = this.customSettings.toMutableMap()
        updatedSettings.remove(key)
        return this.copy(customSettings = updatedSettings)
    }

    /**
     * 여러 커스텀 설정 업데이트
     *
     * @param settings 업데이트할 설정 맵
     * @return 업데이트된 ChatRoomSettings 객체
     */
    fun updateCustomSettings(settings: Map<String, Any>): ChatRoomSettings {
        val updatedSettings = this.customSettings.toMutableMap()
        updatedSettings.putAll(settings)
        return this.copy(customSettings = updatedSettings)
    }

    /**
     * 모든 커스텀 설정 초기화
     *
     * @return 커스텀 설정이 비어있는 ChatRoomSettings 객체
     */
    fun clearCustomSettings(): ChatRoomSettings {
        return this.copy(customSettings = emptyMap())
    }
}
