package com.stark.shoot.adapter.out.persistence.mongodb.repository

import com.stark.shoot.adapter.out.persistence.mongodb.document.message.ChatMessageDocument
import org.bson.types.ObjectId
import org.springframework.data.domain.Pageable
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.mongodb.repository.Query

interface ChatMessageMongoRepository : MongoRepository<ChatMessageDocument, ObjectId> {

    @Query("{ 'roomId': ?0 }")
    fun findByRoomId(roomId: ObjectId, pageable: Pageable = Pageable.unpaged()): List<ChatMessageDocument>

    @Query("{ 'roomId': ?0, '_id': { \$lt: ?1 } }") // 마지막 ID보다 작은 데이터 가져오기
    fun findByRoomIdAndIdBefore(
        roomId: ObjectId,
        lastId: ObjectId,
        pageable: Pageable
    ): List<ChatMessageDocument>

    @Query("{ 'roomId': ?0, 'readBy.?#{[1]}' : { \$ne: true } }")
    fun findUnreadMessages(roomId: ObjectId, userId: String): List<ChatMessageDocument>

}