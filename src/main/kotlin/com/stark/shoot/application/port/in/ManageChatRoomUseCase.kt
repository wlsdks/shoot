package com.stark.shoot.application.port.`in`

interface ManageChatRoomUseCase {
    fun addParticipant(roomId: String, userId: String): Boolean
    fun removeParticipant(roomId: String, userId: String): Boolean
    fun updateRoomSettings(roomId: String, title: String?, notificationEnabled: Boolean?)
}