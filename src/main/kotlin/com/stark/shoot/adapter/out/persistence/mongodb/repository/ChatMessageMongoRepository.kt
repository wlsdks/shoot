package com.stark.shoot.adapter.out.persistence.mongodb.repository

import com.stark.shoot.adapter.out.persistence.mongodb.document.message.ChatMessageDocument
import org.bson.types.ObjectId
import org.springframework.data.domain.Pageable
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.mongodb.repository.Query
import java.time.Instant

interface ChatMessageMongoRepository : MongoRepository<ChatMessageDocument, ObjectId> {

    @Query("{ 'roomId': ?0 }")
    fun findByRoomId(roomId: ObjectId, pageable: Pageable = Pageable.unpaged()): List<ChatMessageDocument>

    @Query("{ 'roomId': ?0, '_id': { \$lt: ?1 } }") // 마지막 ID보다 작은 데이터 가져오기
    fun findByRoomIdAndIdBefore(
        roomId: ObjectId,
        lastId: ObjectId,
        pageable: Pageable
    ): List<ChatMessageDocument>

    fun findByRoomIdAndReadByNotContaining(roomId: ObjectId, userId: ObjectId): List<ChatMessageDocument>

    fun countByRoomId(roomId: ObjectId): Int

    fun countByRoomIdAndCreatedAtAfter(roomId: ObjectId, createdAt: Instant): Int
}