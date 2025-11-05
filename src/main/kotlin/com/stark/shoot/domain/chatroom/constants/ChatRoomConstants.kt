package com.stark.shoot.domain.chatroom.constants

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * ChatRoom Context 전용 상수
 */
@ConfigurationProperties(prefix = "app.domain.chat-room")
data class ChatRoomConstants(
    val maxParticipants: Int = 100,
    val minGroupParticipants: Int = 2,
    val maxPinnedMessages: Int = 5
)
