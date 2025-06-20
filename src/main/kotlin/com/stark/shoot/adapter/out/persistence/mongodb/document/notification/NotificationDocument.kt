package com.stark.shoot.adapter.out.persistence.mongodb.document.notification

import com.stark.shoot.domain.notification.Notification
import com.stark.shoot.domain.notification.vo.NotificationId
import com.stark.shoot.domain.notification.type.NotificationType
import com.stark.shoot.domain.notification.type.SourceType
import com.stark.shoot.domain.notification.vo.NotificationTitle
import com.stark.shoot.domain.notification.vo.NotificationMessage
import com.stark.shoot.domain.user.vo.UserId
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
            id = id?.let { NotificationId.from(it) },
            userId = UserId.from(userId),
            title = NotificationTitle.from(title),
            message = NotificationMessage.from(message),
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
                id = notification.id?.value,
                userId = notification.userId.value,
                title = notification.title.value,
                message = notification.message.value,
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