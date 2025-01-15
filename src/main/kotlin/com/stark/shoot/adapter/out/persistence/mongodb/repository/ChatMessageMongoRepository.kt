package com.stark.shoot.adapter.out.persistence.mongodb.repository

import com.stark.shoot.adapter.out.persistence.mongodb.document.message.ChatMessageDocument
import org.bson.types.ObjectId
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.mongodb.repository.Query
import java.time.Instant

interface ChatMessageMongoRepository : MongoRepository<ChatMessageDocument, ObjectId> {

    fun findByRoomId(roomId: ObjectId): List<ChatMessageDocument>
    fun findByRoomIdAndCreatedAtBeforeOrderByCreatedAtDesc(
        roomId: ObjectId,
        createdAt: Instant
    ): List<ChatMessageDocument>

    @Query("{'roomId': ?0, 'status': {\$ne: 'READ'}}")  // $ 앞에 \ 추가
    fun countUnreadMessages(roomId: ObjectId): Long

}