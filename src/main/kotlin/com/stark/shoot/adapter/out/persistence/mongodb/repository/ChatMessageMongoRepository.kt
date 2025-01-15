package com.stark.shoot.adapter.out.persistence.mongodb.repository

import com.stark.shoot.adapter.out.persistence.mongodb.document.message.ChatMessageDocument
import org.bson.types.ObjectId
import org.springframework.data.mongodb.repository.MongoRepository
import java.time.Instant

interface ChatMessageMongoRepository : MongoRepository<ChatMessageDocument, ObjectId> {

    fun findByRoomIdOrderByCreatedAtDesc(roomId: ObjectId): List<ChatMessageDocument>

    fun findByRoomIdAndCreatedAtBeforeOrderByCreatedAtDesc(
        roomId: ObjectId,
        createdAt: Instant
    ): List<ChatMessageDocument>

    fun countByRoomIdAndStatusNot(roomId: ObjectId, status: String): Long

}