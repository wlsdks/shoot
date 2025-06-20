package com.stark.shoot.adapter.`in`.web.dto.notification

import com.stark.shoot.domain.notification.Notification
import com.stark.shoot.infrastructure.annotation.ApplicationDto
import java.time.Instant

@ApplicationDto
data class NotificationResponse(
    val id: String?,
    val userId: Long,
    val title: String,
    val message: String,
    val type: String,
    val sourceId: String,
    val sourceType: String,
    val isRead: Boolean,
    val createdAt: Instant,
    val readAt: Instant?,
    val metadata: Map<String, Any>
) {
    companion object {
        fun from(notification: Notification): NotificationResponse {
            return NotificationResponse(
                id = notification.id,
                userId = notification.userId,
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

        fun from(notifications: List<Notification>): List<NotificationResponse> {
            return notifications.map { from(it) }
        }
    }

}