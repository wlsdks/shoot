package com.stark.shoot.application.port.out.message

import com.stark.shoot.domain.chat.message.ChatMessage
import kotlinx.coroutines.flow.Flow
import org.bson.types.ObjectId

interface LoadMessagePort {
    fun findById(id: ObjectId): ChatMessage?
    fun findByRoomId(roomId: Long, limit: Int): List<ChatMessage>
    fun findByRoomIdAndBeforeId(roomId: Long, lastId: ObjectId, limit: Int): List<ChatMessage>
    fun findByRoomIdAndAfterId(roomId: Long, lastId: ObjectId, limit: Int): List<ChatMessage> // 추가
    fun findUnreadByRoomId(roomId: Long, userId: Long): List<ChatMessage>
    fun findPinnedMessagesByRoomId(roomId: Long, limit: Int): List<ChatMessage>

    fun findByRoomIdFlow(roomId: Long, limit: Int): Flow<ChatMessage>
    fun findByRoomIdAndBeforeIdFlow(roomId: Long, messageId: ObjectId, limit: Int): Flow<ChatMessage>
    fun findByRoomIdAndAfterIdFlow(roomId: Long, messageId: ObjectId, limit: Int): Flow<ChatMessage>
}