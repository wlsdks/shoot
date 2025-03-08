package com.stark.shoot.application.port.out.message

import com.stark.shoot.domain.chat.message.ChatMessage
import org.bson.types.ObjectId

interface LoadMessagePort {
    fun findById(id: ObjectId): ChatMessage?
    fun findByRoomId(roomId: ObjectId, limit: Int): List<ChatMessage>
    fun findByRoomIdAndBeforeId(roomId: ObjectId, lastId: ObjectId, limit: Int): List<ChatMessage>
    fun findByRoomIdAndAfterId(roomId: ObjectId, lastId: ObjectId, limit: Int): List<ChatMessage> // 추가
    fun findUnreadByRoomId(roomId: ObjectId, userId: ObjectId): List<ChatMessage>
    fun findPinnedMessagesByRoomId(roomId: ObjectId, limit: Int): List<ChatMessage>
}