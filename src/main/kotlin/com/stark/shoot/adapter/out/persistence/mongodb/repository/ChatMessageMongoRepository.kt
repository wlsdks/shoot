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

    fun countByRoomId(roomId: ObjectId): Int

    fun countByRoomIdAndCreatedAtAfter(roomId: ObjectId, lastReadMessageCreatedAt: Instant): Int

    fun findByRoomIdAndReadByNotContaining(roomId: ObjectId, userId: ObjectId): List<ChatMessageDocument>

}