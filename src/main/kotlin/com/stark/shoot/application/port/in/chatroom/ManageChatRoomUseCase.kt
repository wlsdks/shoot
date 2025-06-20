package com.stark.shoot.application.port.`in`.chatroom

import com.stark.shoot.domain.chat.room.vo.ChatRoomAnnouncement
import com.stark.shoot.domain.chat.room.vo.ChatRoomId
import com.stark.shoot.domain.chat.room.vo.ChatRoomTitle
import com.stark.shoot.domain.common.vo.UserId

interface ManageChatRoomUseCase {
    fun addParticipant(roomId: ChatRoomId, userId: UserId): Boolean
    fun removeParticipant(roomId: ChatRoomId, userId: UserId): Boolean
    fun updateAnnouncement(roomId: ChatRoomId, announcement: ChatRoomAnnouncement?)
    fun updateTitle(roomId: ChatRoomId, title: ChatRoomTitle): Boolean
}
