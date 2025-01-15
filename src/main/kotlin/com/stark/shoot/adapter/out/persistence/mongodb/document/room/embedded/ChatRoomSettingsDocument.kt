package com.stark.shoot.adapter.out.persistence.mongodb.document.room.embedded

data class ChatRoomSettingsDocument(
    val isNotificationEnabled: Boolean = true,    // 알림 설정
    val retentionDays: Int? = null,              // 메시지 보관 기간
    val isEncrypted: Boolean = false,            // 암호화 여부
    val customSettings: Map<String, Any> = emptyMap() // 추가 설정들을 유연하게 저장
)