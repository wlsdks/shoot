package com.stark.shoot.application.port.out.message

import com.stark.shoot.domain.chat.message.ChatMessage
import kotlinx.coroutines.flow.Flow
import org.bson.types.ObjectId

interface LoadMessagePort {
    fun findById(id: ObjectId): ChatMessage?
    fun findByRoomId(roomId: ObjectId, limit: Int): List<ChatMessage>
    fun findByRoomIdAndBeforeId(roomId: ObjectId, lastId: ObjectId, limit: Int): List<ChatMessage>
    fun findByRoomIdAndAfterId(roomId: ObjectId, lastId: ObjectId, limit: Int): List<ChatMessage> // 추가
    fun findUnreadByRoomId(roomId: Long, userId: Long): List<ChatMessage>
    fun findPinnedMessagesByRoomId(roomId: ObjectId, limit: Int): List<ChatMessage>

    fun findByRoomIdFlow(roomId: ObjectId, limit: Int): Flow<ChatMessage>
    fun findByRoomIdAndBeforeIdFlow(roomId: ObjectId, messageId: ObjectId, limit: Int): Flow<ChatMessage>
    fun findByRoomIdAndAfterIdFlow(roomId: ObjectId, messageId: ObjectId, limit: Int): Flow<ChatMessage>
}