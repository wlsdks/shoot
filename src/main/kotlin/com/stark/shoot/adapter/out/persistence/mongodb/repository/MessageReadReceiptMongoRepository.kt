package com.stark.shoot.adapter.out.persistence.mongodb.repository

import com.stark.shoot.adapter.out.persistence.mongodb.document.message.readreceipt.MessageReadReceiptDocument
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.mongodb.repository.Query

interface MessageReadReceiptMongoRepository : MongoRepository<MessageReadReceiptDocument, Long> {

    @Query("{ 'messageId': ?0, 'userId': ?1 }")
    fun findByMessageIdAndUserId(messageId: String, userId: Long): MessageReadReceiptDocument?

    @Query("{ 'messageId': ?0 }")
    fun findAllByMessageId(messageId: String): List<MessageReadReceiptDocument>

    @Query("{ 'roomId': ?0 }")
    fun findAllByRoomId(roomId: Long): List<MessageReadReceiptDocument>

    @Query("{ 'roomId': ?0, 'userId': ?1 }")
    fun findAllByRoomIdAndUserId(roomId: Long, userId: Long): List<MessageReadReceiptDocument>

    fun countByMessageId(messageId: String): Long

    fun deleteByMessageId(messageId: String)

    fun deleteAllByRoomId(roomId: Long)

    @Query("{ 'messageId': ?0, 'userId': ?1 }")
    fun existsByMessageIdAndUserId(messageId: String, userId: Long): Boolean

    fun deleteByMessageIdAndUserId(messageId: String, userId: Long)
}
