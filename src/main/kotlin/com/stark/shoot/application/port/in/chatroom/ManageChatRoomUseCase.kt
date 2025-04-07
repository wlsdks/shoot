package com.stark.shoot.application.port.`in`.chatroom

interface ManageChatRoomUseCase {
    fun addParticipant(roomId: Long, userId: Long): Boolean
    fun removeParticipant(roomId: Long, userId: Long): Boolean
    fun updateAnnouncement(roomId: Long, announcement: String?)
}