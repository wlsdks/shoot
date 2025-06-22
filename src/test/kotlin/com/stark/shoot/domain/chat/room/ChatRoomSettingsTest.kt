package com.stark.shoot.domain.chat.room

/** 새 값 객체 사용을 위한 import */
import com.stark.shoot.domain.chat.room.RetentionDays
import com.stark.shoot.domain.chatroom.ChatRoomSettings
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("채팅방 설정 테스트")
class ChatRoomSettingsTest {

    @Nested
    @DisplayName("채팅방 설정 생성 시")
    inner class CreateChatRoomSettings {
    
        @Test
        @DisplayName("기본 값으로 채팅방 설정을 생성할 수 있다")
        fun `기본 값으로 채팅방 설정을 생성할 수 있다`() {
            // when
            val settings = ChatRoomSettings()
            
            // then
            assertThat(settings.isNotificationEnabled).isTrue()
            assertThat(settings.retentionDays).isNull()
            assertThat(settings.isEncrypted).isFalse()
            assertThat(settings.customSettings).isEmpty()
        }
        
        @Test
        @DisplayName("사용자 지정 값으로 채팅방 설정을 생성할 수 있다")
        fun `사용자 지정 값으로 채팅방 설정을 생성할 수 있다`() {
            // given
            val isNotificationEnabled = false
            val retentionDays = RetentionDays.from(30)
            val isEncrypted = true
            val customSettings = mapOf("theme" to "dark", "fontSize" to 14)
            
            // when
            val settings = ChatRoomSettings(
                isNotificationEnabled = isNotificationEnabled,
                retentionDays = retentionDays,
                isEncrypted = isEncrypted,
                customSettings = customSettings
            )
            
            // then
            assertThat(settings.isNotificationEnabled).isEqualTo(isNotificationEnabled)
            assertThat(settings.retentionDays).isEqualTo(retentionDays)
            assertThat(settings.isEncrypted).isEqualTo(isEncrypted)
            assertThat(settings.customSettings).isEqualTo(customSettings)
        }
    }
    
    @Nested
    @DisplayName("알림 설정 업데이트 시")
    inner class UpdateNotificationSettings {
    
        @Test
        @DisplayName("알림을 비활성화할 수 있다")
        fun `알림을 비활성화할 수 있다`() {
            // given
            val settings = ChatRoomSettings(isNotificationEnabled = true)
            
            // when
            val updatedSettings = settings.updateNotificationSettings(false)
            
            // then
            assertThat(updatedSettings.isNotificationEnabled).isFalse()
            // 다른 설정은 변경되지 않아야 함
            assertThat(updatedSettings.retentionDays).isEqualTo(settings.retentionDays)
            assertThat(updatedSettings.isEncrypted).isEqualTo(settings.isEncrypted)
            assertThat(updatedSettings.customSettings).isEqualTo(settings.customSettings)
        }
        
        @Test
        @DisplayName("알림을 활성화할 수 있다")
        fun `알림을 활성화할 수 있다`() {
            // given
            val settings = ChatRoomSettings(isNotificationEnabled = false)
            
            // when
            val updatedSettings = settings.updateNotificationSettings(true)
            
            // then
            assertThat(updatedSettings.isNotificationEnabled).isTrue()
            // 다른 설정은 변경되지 않아야 함
            assertThat(updatedSettings.retentionDays).isEqualTo(settings.retentionDays)
            assertThat(updatedSettings.isEncrypted).isEqualTo(settings.isEncrypted)
            assertThat(updatedSettings.customSettings).isEqualTo(settings.customSettings)
        }
    }
    
    @Nested
    @DisplayName("메시지 보존 기간 설정 시")
    inner class UpdateRetentionPolicy {
    
        @Test
        @DisplayName("메시지 보존 기간을 설정할 수 있다")
        fun `메시지 보존 기간을 설정할 수 있다`() {
            // given
            val settings = ChatRoomSettings(retentionDays = null)
            val newRetentionDays = RetentionDays.from(14)
            
            // when
            val updatedSettings = settings.updateRetentionPolicy(newRetentionDays)
            
            // then
            assertThat(updatedSettings.retentionDays).isEqualTo(newRetentionDays)
            // 다른 설정은 변경되지 않아야 함
            assertThat(updatedSettings.isNotificationEnabled).isEqualTo(settings.isNotificationEnabled)
            assertThat(updatedSettings.isEncrypted).isEqualTo(settings.isEncrypted)
            assertThat(updatedSettings.customSettings).isEqualTo(settings.customSettings)
        }
        
        @Test
        @DisplayName("메시지 보존 기간을 무기한으로 설정할 수 있다")
        fun `메시지 보존 기간을 무기한으로 설정할 수 있다`() {
            // given
            val settings = ChatRoomSettings(retentionDays = RetentionDays.from(30))
            
            // when
            val updatedSettings = settings.updateRetentionPolicy(null)
            
            // then
            assertThat(updatedSettings.retentionDays).isNull()
            // 다른 설정은 변경되지 않아야 함
            assertThat(updatedSettings.isNotificationEnabled).isEqualTo(settings.isNotificationEnabled)
            assertThat(updatedSettings.isEncrypted).isEqualTo(settings.isEncrypted)
            assertThat(updatedSettings.customSettings).isEqualTo(settings.customSettings)
        }
    }
    
    @Nested
    @DisplayName("암호화 설정 업데이트 시")
    inner class UpdateEncryption {
    
        @Test
        @DisplayName("암호화를 활성화할 수 있다")
        fun `암호화를 활성화할 수 있다`() {
            // given
            val settings = ChatRoomSettings(isEncrypted = false)
            
            // when
            val updatedSettings = settings.updateEncryption(true)
            
            // then
            assertThat(updatedSettings.isEncrypted).isTrue()
            // 다른 설정은 변경되지 않아야 함
            assertThat(updatedSettings.isNotificationEnabled).isEqualTo(settings.isNotificationEnabled)
            assertThat(updatedSettings.retentionDays).isEqualTo(settings.retentionDays)
            assertThat(updatedSettings.customSettings).isEqualTo(settings.customSettings)
        }
        
        @Test
        @DisplayName("암호화를 비활성화할 수 있다")
        fun `암호화를 비활성화할 수 있다`() {
            // given
            val settings = ChatRoomSettings(isEncrypted = true)
            
            // when
            val updatedSettings = settings.updateEncryption(false)
            
            // then
            assertThat(updatedSettings.isEncrypted).isFalse()
            // 다른 설정은 변경되지 않아야 함
            assertThat(updatedSettings.isNotificationEnabled).isEqualTo(settings.isNotificationEnabled)
            assertThat(updatedSettings.retentionDays).isEqualTo(settings.retentionDays)
            assertThat(updatedSettings.customSettings).isEqualTo(settings.customSettings)
        }
    }
    
    @Nested
    @DisplayName("커스텀 설정 관리 시")
    inner class ManageCustomSettings {
    
        @Test
        @DisplayName("커스텀 설정을 추가할 수 있다")
        fun `커스텀 설정을 추가할 수 있다`() {
            // given
            val settings = ChatRoomSettings()
            val key = "theme"
            val value = "dark"
            
            // when
            val updatedSettings = settings.addCustomSetting(key, value)
            
            // then
            assertThat(updatedSettings.customSettings).hasSize(1)
            assertThat(updatedSettings.customSettings).containsEntry(key, value)
            // 다른 설정은 변경되지 않아야 함
            assertThat(updatedSettings.isNotificationEnabled).isEqualTo(settings.isNotificationEnabled)
            assertThat(updatedSettings.retentionDays).isEqualTo(settings.retentionDays)
            assertThat(updatedSettings.isEncrypted).isEqualTo(settings.isEncrypted)
        }
        
        @Test
        @DisplayName("기존 커스텀 설정을 덮어쓸 수 있다")
        fun `기존 커스텀 설정을 덮어쓸 수 있다`() {
            // given
            val initialCustomSettings = mapOf("theme" to "light", "fontSize" to 12)
            val settings = ChatRoomSettings(customSettings = initialCustomSettings)
            val key = "theme"
            val newValue = "dark"
            
            // when
            val updatedSettings = settings.addCustomSetting(key, newValue)
            
            // then
            assertThat(updatedSettings.customSettings).hasSize(2)
            assertThat(updatedSettings.customSettings).containsEntry(key, newValue)
            assertThat(updatedSettings.customSettings).containsEntry("fontSize", 12)
            // 다른 설정은 변경되지 않아야 함
            assertThat(updatedSettings.isNotificationEnabled).isEqualTo(settings.isNotificationEnabled)
            assertThat(updatedSettings.retentionDays).isEqualTo(settings.retentionDays)
            assertThat(updatedSettings.isEncrypted).isEqualTo(settings.isEncrypted)
        }
        
        @Test
        @DisplayName("커스텀 설정을 제거할 수 있다")
        fun `커스텀 설정을 제거할 수 있다`() {
            // given
            val initialCustomSettings = mapOf("theme" to "dark", "fontSize" to 14)
            val settings = ChatRoomSettings(customSettings = initialCustomSettings)
            val keyToRemove = "theme"
            
            // when
            val updatedSettings = settings.removeCustomSetting(keyToRemove)
            
            // then
            assertThat(updatedSettings.customSettings).hasSize(1)
            assertThat(updatedSettings.customSettings).doesNotContainKey(keyToRemove)
            assertThat(updatedSettings.customSettings).containsEntry("fontSize", 14)
            // 다른 설정은 변경되지 않아야 함
            assertThat(updatedSettings.isNotificationEnabled).isEqualTo(settings.isNotificationEnabled)
            assertThat(updatedSettings.retentionDays).isEqualTo(settings.retentionDays)
            assertThat(updatedSettings.isEncrypted).isEqualTo(settings.isEncrypted)
        }
        
        @Test
        @DisplayName("존재하지 않는 커스텀 설정을 제거해도 오류가 발생하지 않는다")
        fun `존재하지 않는 커스텀 설정을 제거해도 오류가 발생하지 않는다`() {
            // given
            val initialCustomSettings = mapOf("theme" to "dark")
            val settings = ChatRoomSettings(customSettings = initialCustomSettings)
            val nonExistingKey = "fontSize"
            
            // when
            val updatedSettings = settings.removeCustomSetting(nonExistingKey)
            
            // then
            assertThat(updatedSettings.customSettings).isEqualTo(initialCustomSettings)
            // 다른 설정은 변경되지 않아야 함
            assertThat(updatedSettings.isNotificationEnabled).isEqualTo(settings.isNotificationEnabled)
            assertThat(updatedSettings.retentionDays).isEqualTo(settings.retentionDays)
            assertThat(updatedSettings.isEncrypted).isEqualTo(settings.isEncrypted)
        }
        
        @Test
        @DisplayName("여러 커스텀 설정을 한번에 업데이트할 수 있다")
        fun `여러 커스텀 설정을 한번에 업데이트할 수 있다`() {
            // given
            val initialCustomSettings = mapOf("theme" to "light", "fontSize" to 12)
            val settings = ChatRoomSettings(customSettings = initialCustomSettings)
            val newSettings = mapOf("theme" to "dark", "language" to "ko")
            
            // when
            val updatedSettings = settings.updateCustomSettings(newSettings)
            
            // then
            assertThat(updatedSettings.customSettings).hasSize(3)
            assertThat(updatedSettings.customSettings).containsEntry("theme", "dark")
            assertThat(updatedSettings.customSettings).containsEntry("fontSize", 12)
            assertThat(updatedSettings.customSettings).containsEntry("language", "ko")
            // 다른 설정은 변경되지 않아야 함
            assertThat(updatedSettings.isNotificationEnabled).isEqualTo(settings.isNotificationEnabled)
            assertThat(updatedSettings.retentionDays).isEqualTo(settings.retentionDays)
            assertThat(updatedSettings.isEncrypted).isEqualTo(settings.isEncrypted)
        }
        
        @Test
        @DisplayName("모든 커스텀 설정을 초기화할 수 있다")
        fun `모든 커스텀 설정을 초기화할 수 있다`() {
            // given
            val initialCustomSettings = mapOf("theme" to "dark", "fontSize" to 14, "language" to "ko")
            val settings = ChatRoomSettings(customSettings = initialCustomSettings)
            
            // when
            val updatedSettings = settings.clearCustomSettings()
            
            // then
            assertThat(updatedSettings.customSettings).isEmpty()
            // 다른 설정은 변경되지 않아야 함
            assertThat(updatedSettings.isNotificationEnabled).isEqualTo(settings.isNotificationEnabled)
            assertThat(updatedSettings.retentionDays).isEqualTo(settings.retentionDays)
            assertThat(updatedSettings.isEncrypted).isEqualTo(settings.isEncrypted)
        }
    }
}