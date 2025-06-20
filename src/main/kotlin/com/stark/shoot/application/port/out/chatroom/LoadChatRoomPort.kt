package com.stark.shoot.application.port.out.chatroom

import com.stark.shoot.domain.chat.room.ChatRoom
import com.stark.shoot.domain.chat.room.ChatRoomId

interface LoadChatRoomPort {
    fun findById(roomId: ChatRoomId): ChatRoom?
    fun findByParticipantId(participantId: Long): List<ChatRoom>
}