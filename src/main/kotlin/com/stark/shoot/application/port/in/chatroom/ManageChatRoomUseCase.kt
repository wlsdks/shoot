package com.stark.shoot.application.port.`in`.chatroom

import com.stark.shoot.domain.chat.room.ChatRoomAnnouncement

interface ManageChatRoomUseCase {
    fun addParticipant(roomId: Long, userId: Long): Boolean
    fun removeParticipant(roomId: Long, userId: Long): Boolean
    fun updateAnnouncement(roomId: Long, announcement: ChatRoomAnnouncement?)

    /**
     * 채팅방 제목을 업데이트합니다.
     *
     * @param roomId 채팅방 ID
     * @param title 새로운 채팅방 제목
     * @return 업데이트 성공 여부
     */
    fun updateTitle(roomId: Long, title: String): Boolean
}
