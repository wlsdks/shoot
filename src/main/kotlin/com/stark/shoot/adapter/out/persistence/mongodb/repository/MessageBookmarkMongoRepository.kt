package com.stark.shoot.adapter.out.persistence.mongodb.repository

import com.stark.shoot.adapter.out.persistence.mongodb.document.message.bookmark.MessageBookmarkDocument
import org.springframework.data.mongodb.repository.MongoRepository

interface MessageBookmarkMongoRepository : MongoRepository<MessageBookmarkDocument, String> {
    fun findByUserId(userId: Long): List<MessageBookmarkDocument>
    fun existsByMessageIdAndUserId(messageId: String, userId: Long): Boolean
    fun deleteByMessageIdAndUserId(messageId: String, userId: Long)
}
