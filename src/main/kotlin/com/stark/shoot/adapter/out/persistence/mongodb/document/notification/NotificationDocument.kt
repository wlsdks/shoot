package com.stark.shoot.adapter.out.persistence.mongodb.document.notification

import com.stark.shoot.domain.notification.Notification
import com.stark.shoot.domain.notification.NotificationType
import com.stark.shoot.domain.notification.SourceType
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

@Document(collection = "notifications")
data class NotificationDocument(
    @Id
    val id: String? = null,

    @Indexed
    val userId: Long,

    val title: String,
    val message: String,

    @Indexed
    val type: String,

    @Indexed
    val sourceId: String,

    @Indexed
    val sourceType: String,

    @Indexed
    val isRead: Boolean = false,

    val createdAt: Instant = Instant.now(),
    val readAt: Instant? = null,

    val metadata: Map<String, Any> = emptyMap()
) {

    fun toDomain(): Notification {
        return Notification(
            id = id,
            userId = userId,
            title = title,
            message = message,
            type = NotificationType.valueOf(type),
            sourceId = sourceId,
            sourceType = SourceType.valueOf(sourceType),
            isRead = isRead,
            createdAt = createdAt,
            readAt = readAt,
            metadata = metadata
        )
    }

    companion object {
        fun fromDomain(notification: Notification): NotificationDocument {
            return NotificationDocument(
                id = notification.id,
                userId = notification.userId,
                title = notification.title,
                message = notification.message,
                type = notification.type.name,
                sourceId = notification.sourceId,
                sourceType = notification.sourceType.name,
                isRead = notification.isRead,
                createdAt = notification.createdAt,
                readAt = notification.readAt,
                metadata = notification.metadata
            )
        }
    }

}