package com.stark.shoot.adapter.out.persistence.mongodb.repository

import com.stark.shoot.adapter.out.persistence.mongodb.document.message.pin.MessagePinDocument
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.mongodb.repository.Query

interface MessagePinMongoRepository : MongoRepository<MessagePinDocument, Long> {

    @Query("{ 'messageId': ?0 }")
    fun findByMessageId(messageId: String): MessagePinDocument?

    @Query("{ 'roomId': ?0 }")
    fun findAllByRoomId(roomId: Long): List<MessagePinDocument>

    fun countByRoomId(roomId: Long): Long

    fun deleteByMessageId(messageId: String)

    fun deleteAllByRoomId(roomId: Long)
}
