package com.stark.shoot.application.port.out.message

import com.stark.shoot.domain.chat.message.ChatMessage
import kotlinx.coroutines.flow.Flow
import org.bson.types.ObjectId

interface LoadMessagePort {
    fun findById(id: ObjectId): ChatMessage?
    fun findByRoomId(roomId: Long, limit: Int): List<ChatMessage>
    fun findByRoomIdAndBeforeId(roomId: Long, lastId: ObjectId, limit: Int): List<ChatMessage>
    fun findByRoomIdAndAfterId(roomId: Long, lastId: ObjectId, limit: Int): List<ChatMessage> // 추가
    /**
     * 채팅방 ID와 사용자 ID로 읽지 않은 메시지 조회 (페이지네이션 적용)
     *
     * @param roomId 채팅방 ID
     * @param userId 사용자 ID
     * @param limit 한 번에 조회할 최대 메시지 수 (기본값: 100)
     * @return 읽지 않은 메시지 목록
     */
    fun findUnreadByRoomId(roomId: Long, userId: Long, limit: Int = 100): List<ChatMessage>
    fun findPinnedMessagesByRoomId(roomId: Long, limit: Int): List<ChatMessage>

    fun findByThreadId(threadId: ObjectId, limit: Int): List<ChatMessage>
    fun findByThreadIdAndBeforeId(threadId: ObjectId, lastId: ObjectId, limit: Int): List<ChatMessage>

    fun findThreadRootsByRoomId(roomId: Long, limit: Int): List<ChatMessage>
    fun findThreadRootsByRoomIdAndBeforeId(roomId: Long, lastId: ObjectId, limit: Int): List<ChatMessage>
    fun countByThreadId(threadId: ObjectId): Long

    fun findByRoomIdFlow(roomId: Long, limit: Int): Flow<ChatMessage>
    fun findByRoomIdAndBeforeIdFlow(roomId: Long, messageId: ObjectId, limit: Int): Flow<ChatMessage>
    fun findByRoomIdAndAfterIdFlow(roomId: Long, messageId: ObjectId, limit: Int): Flow<ChatMessage>
}
