package com.stark.shoot.adapter.out.persistence.mongodb.repository

import com.stark.shoot.adapter.out.persistence.mongodb.document.message.ChatMessageDocument
import org.bson.types.ObjectId
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.mongodb.repository.Query
import java.time.Instant

interface ChatMessageMongoRepository : MongoRepository<ChatMessageDocument, ObjectId> {

    fun findByRoomId(roomId: ObjectId): List<ChatMessageDocument>

    @Query("{ 'roomId': ?0, 'createdAt': { \$lt: ?1 } }")
    fun findByRoomIdAndCreatedAtBeforeOrderByCreatedAtDesc(
        roomId: ObjectId,
        before: Instant
    ): List<ChatMessageDocument>

    @Query("{'roomId': ?0, 'status': {\$ne: 'READ'}}")  // $ 앞에 \ 추가
    fun countUnreadMessages(roomId: ObjectId): Long

    fun countByRoomId(roomId: ObjectId): Int

    fun countByRoomIdAndIdGreaterThan(roomId: ObjectId, lastReadMessageId: ObjectId): Int

    // 특정 채팅방에서 읽지 않은 메시지 수 계산
    fun countByRoomIdAndCreatedAtAfter(roomId: ObjectId, lastReadMessageCreatedAt: Instant): Int

}