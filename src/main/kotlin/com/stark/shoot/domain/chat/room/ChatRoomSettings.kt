package com.stark.shoot.domain.chat.room

data class ChatRoomSettings(
    val isNotificationEnabled: Boolean = true,
    val retentionDays: Int? = null,
    val isEncrypted: Boolean = false,
    val customSettings: Map<String, Any> = emptyMap()
)