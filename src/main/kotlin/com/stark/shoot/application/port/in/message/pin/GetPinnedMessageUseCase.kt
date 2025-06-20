package com.stark.shoot.application.port.`in`.message.pin

import com.stark.shoot.domain.chat.message.ChatMessage
import com.stark.shoot.domain.chat.room.vo.ChatRoomId

interface GetPinnedMessageUseCase {
    fun getPinnedMessages(roomId: ChatRoomId): List<ChatMessage>
}