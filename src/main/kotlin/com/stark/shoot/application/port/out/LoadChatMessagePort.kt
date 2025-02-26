package com.stark.shoot.application.port.out

import com.stark.shoot.domain.chat.message.ChatMessage
import org.bson.types.ObjectId

interface LoadChatMessagePort {
    fun findById(id: ObjectId): ChatMessage?
    fun findByRoomId(roomId: ObjectId, limit: Int): List<ChatMessage>
    fun findByRoomIdAndBeforeId(roomId: ObjectId, lastId: ObjectId, limit: Int): List<ChatMessage>
    fun findUnreadByRoomId(roomId: ObjectId, userId: ObjectId): List<ChatMessage>
}