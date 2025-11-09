package com.stark.shoot.domain.chatroom.constants

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * ChatRoom Context 전용 상수
 */
@ConfigurationProperties(prefix = "app.domain.chat-room")
data class ChatRoomConstants(
    val maxParticipants: Int = 100,
    val minGroupParticipants: Int = 2,
    val maxPinnedMessages: Int = 5,
    val maxPinnedChatRooms: Int = 20  // 사용자당 최대 고정 가능한 채팅방 수
)
