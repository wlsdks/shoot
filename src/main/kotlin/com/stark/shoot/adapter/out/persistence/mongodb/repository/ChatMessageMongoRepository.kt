package com.stark.shoot.adapter.out.persistence.mongodb.repository

import com.stark.shoot.adapter.out.persistence.mongodb.document.message.ChatMessageDocument
import org.bson.types.ObjectId
import org.springframework.data.domain.Pageable
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.mongodb.repository.Query

interface ChatMessageMongoRepository : MongoRepository<ChatMessageDocument, ObjectId> {

    @Query("{ 'roomId': ?0 }")
    fun findByRoomId(roomId: Long, pageable: Pageable = Pageable.unpaged()): List<ChatMessageDocument>

    @Query("{ 'roomId': ?0, '_id': { \$lt: ?1 } }") // 마지막 ID보다 작은 데이터 가져오기
    fun findByRoomIdAndIdBefore(
        roomId: Long,
        lastId: ObjectId,
        pageable: Pageable
    ): List<ChatMessageDocument>

    @Query("{ 'roomId': ?0, '_id': { \$gt: ?1 } }") // 마지막 ID보다 큰 데이터 가져오기
    fun findByRoomIdAndIdAfter(
        roomId: Long,
        lastId: ObjectId,
        pageable: Pageable
    ): List<ChatMessageDocument>

    @Query("{ 'roomId': ?0, 'readBy.?#{[1]}' : { \$ne: true } }")
    fun findUnreadMessages(roomId: Long, userId: Long, pageable: Pageable = Pageable.unpaged()): List<ChatMessageDocument>

    @Query("{ 'roomId': ?0, 'isPinned': true }")
    fun findPinnedMessagesByRoomId(roomId: Long, pageable: Pageable = Pageable.unpaged()): List<ChatMessageDocument>

    @Query("{ 'threadId': ?0 }")
    fun findByThreadId(threadId: ObjectId, pageable: Pageable = Pageable.unpaged()): List<ChatMessageDocument>

    @Query("{ 'threadId': ?0, '_id': { \$lt: ?1 } }")
    fun findByThreadIdAndIdBefore(
        threadId: ObjectId,
        lastId: ObjectId,
        pageable: Pageable
    ): List<ChatMessageDocument>

}
