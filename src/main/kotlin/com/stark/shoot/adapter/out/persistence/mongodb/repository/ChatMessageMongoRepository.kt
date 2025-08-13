package com.stark.shoot.adapter.out.persistence.mongodb.repository

import com.stark.shoot.adapter.out.persistence.mongodb.document.message.ChatMessageDocument
import org.bson.types.ObjectId
import org.springframework.data.domain.Pageable
import org.springframework.data.mongodb.repository.Aggregation
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

    @Query("{ 'roomId': ?0, 'threadId': null }")
    fun findThreadRootsByRoomId(roomId: Long, pageable: Pageable = Pageable.unpaged()): List<ChatMessageDocument>

    @Query("{ 'roomId': ?0, '_id': { \$lt: ?1 }, 'threadId': null }")
    fun findThreadRootsByRoomIdAndIdBefore(
        roomId: Long,
        lastId: ObjectId,
        pageable: Pageable
    ): List<ChatMessageDocument>

    fun countByThreadId(threadId: ObjectId): Long

    /**
     * 여러 스레드 ID에 대한 답글 수를 배치로 조회합니다.
     * N+1 쿼리 문제를 해결하기 위해 사용됩니다.
     */
    @Aggregation(pipeline = [
        "{ '\$match': { 'threadId': { '\$in': ?0 } } }",
        "{ '\$group': { '_id': '\$threadId', 'count': { '\$sum': 1 } } }"
    ])
    fun countByThreadIds(threadIds: List<ObjectId>): List<ThreadCountResult>

    /**
     * 스레드별 답글 수 결과를 담는 데이터 클래스
     */
    data class ThreadCountResult(
        val _id: ObjectId,
        val count: Long
    )

}
