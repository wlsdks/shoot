package com.stark.shoot.application.port.`in`.chatroom

import com.stark.shoot.domain.chatroom.vo.ChatRoomAnnouncement
import com.stark.shoot.domain.chatroom.vo.ChatRoomId
import com.stark.shoot.domain.chatroom.vo.ChatRoomTitle
import com.stark.shoot.domain.user.vo.UserId

interface ManageChatRoomUseCase {
    fun addParticipant(roomId: ChatRoomId, userId: UserId): Boolean
    fun removeParticipant(roomId: ChatRoomId, userId: UserId): Boolean
    fun updateAnnouncement(roomId: ChatRoomId, announcement: ChatRoomAnnouncement?)
    fun updateTitle(roomId: ChatRoomId, title: ChatRoomTitle): Boolean
}
