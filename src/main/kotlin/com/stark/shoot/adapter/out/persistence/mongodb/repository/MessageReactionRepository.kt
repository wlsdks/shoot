package com.stark.shoot.adapter.out.persistence.mongodb.repository

import com.stark.shoot.adapter.out.persistence.mongodb.document.message.reaction.MessageReactionDocument
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.mongodb.repository.Query

/**
 * 메시지 리액션 MongoDB Repository
 */
interface MessageReactionRepository : MongoRepository<MessageReactionDocument, String> {

    /**
     * 메시지와 사용자로 리액션 조회
     * 사용자는 메시지당 1개의 리액션만 가질 수 있음
     *
     * @param messageId 메시지 ID
     * @param userId 사용자 ID
     * @return 조회된 리액션 또는 null
     */
    fun findByMessageIdAndUserId(messageId: String, userId: Long): MessageReactionDocument?

    /**
     * 메시지의 모든 리액션 조회
     *
     * @param messageId 메시지 ID
     * @return 리액션 목록
     */
    fun findAllByMessageId(messageId: String): List<MessageReactionDocument>

    /**
     * 메시지의 특정 타입 리액션 조회
     *
     * @param messageId 메시지 ID
     * @param reactionType 리액션 타입
     * @return 리액션 목록
     */
    fun findAllByMessageIdAndReactionType(
        messageId: String,
        reactionType: String
    ): List<MessageReactionDocument>

    /**
     * 메시지와 사용자로 리액션 삭제
     *
     * @param messageId 메시지 ID
     * @param userId 사용자 ID
     */
    fun deleteByMessageIdAndUserId(messageId: String, userId: Long)

    /**
     * 메시지의 모든 리액션 삭제
     *
     * @param messageId 메시지 ID
     */
    fun deleteAllByMessageId(messageId: String)

    /**
     * 메시지의 리액션 개수 조회
     *
     * @param messageId 메시지 ID
     * @return 리액션 개수
     */
    fun countByMessageId(messageId: String): Long

    /**
     * 메시지의 특정 타입 리액션 개수 조회
     *
     * @param messageId 메시지 ID
     * @param reactionType 리액션 타입
     * @return 리액션 개수
     */
    fun countByMessageIdAndReactionType(messageId: String, reactionType: String): Long

    /**
     * 메시지의 리액션 요약 조회
     * MongoDB Aggregation을 사용하여 타입별 개수 집계
     *
     * @param messageId 메시지 ID
     * @return 리액션 타입별 개수 맵
     */
    @Query(
        """
        {
            'messageId': ?0
        }
        """
    )
    fun findReactionsByMessageId(messageId: String): List<MessageReactionDocument>
}
