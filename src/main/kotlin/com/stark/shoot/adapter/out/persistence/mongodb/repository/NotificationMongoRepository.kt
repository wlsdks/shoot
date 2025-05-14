package com.stark.shoot.adapter.out.persistence.mongodb.repository

import com.stark.shoot.adapter.out.persistence.mongodb.document.notification.NotificationDocument
import org.springframework.data.domain.Pageable
import org.springframework.data.mongodb.repository.MongoRepository

interface NotificationMongoRepository : MongoRepository<NotificationDocument, String> {

    fun findByUserId(userId: Long, pageable: Pageable): List<NotificationDocument>
    fun findByUserIdAndIsReadFalse(userId: Long, pageable: Pageable): List<NotificationDocument>
    fun findByUserIdAndType(userId: Long, type: String, pageable: Pageable): List<NotificationDocument>
    fun findByUserIdAndSourceType(userId: Long, sourceType: String, pageable: Pageable): List<NotificationDocument>
    fun findByUserIdAndSourceTypeAndSourceId(
        userId: Long,
        sourceType: String,
        sourceId: String,
        pageable: Pageable
    ): List<NotificationDocument>

    fun countByUserIdAndIsReadFalse(userId: Long): Int
    fun deleteByUserId(userId: Long): Int

}