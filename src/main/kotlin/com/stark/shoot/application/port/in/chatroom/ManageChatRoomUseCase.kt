package com.stark.shoot.application.port.`in`.chatroom

import org.bson.types.ObjectId

interface ManageChatRoomUseCase {
    fun addParticipant(roomId: String, userId: ObjectId): Boolean
    fun removeParticipant(roomId: String, userId: ObjectId): Boolean
    fun updateRoomSettings(roomId: String, title: String?, notificationEnabled: Boolean?)
}