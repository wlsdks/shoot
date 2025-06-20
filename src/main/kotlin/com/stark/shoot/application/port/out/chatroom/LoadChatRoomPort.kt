package com.stark.shoot.application.port.out.chatroom

import com.stark.shoot.domain.chat.room.ChatRoom
import com.stark.shoot.domain.chat.room.ChatRoomId
import com.stark.shoot.domain.common.vo.UserId

interface LoadChatRoomPort {
    fun findById(roomId: ChatRoomId): ChatRoom?
    fun findByParticipantId(participantId: UserId): List<ChatRoom>
}