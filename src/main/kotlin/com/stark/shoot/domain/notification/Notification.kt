package com.stark.shoot.domain.notification

import java.time.Instant

data class Notification(
    val id: String? = null,
    val userId: Long,
    val title: String,
    val message: String,
    val type: NotificationType,
    val sourceId: String,
    val sourceType: SourceType,
    val isRead: Boolean = false,
    val createdAt: Instant = Instant.now(),
    val readAt: Instant? = null,
    val metadata: Map<String, Any> = emptyMap()
) {

    fun markAsRead(): Notification {
        return if (isRead) {
            this
        } else {
            this.copy(isRead = true, readAt = Instant.now())
        }
    }

    companion object {
        fun fromChatEvent(
            userId: Long,
            title: String,
            message: String,
            type: NotificationType,
            sourceId: String,
            metadata: Map<String, Any> = emptyMap()
        ): Notification {
            return Notification(
                userId = userId,
                title = title,
                message = message,
                type = type,
                sourceId = sourceId,
                sourceType = SourceType.CHAT,
                metadata = metadata
            )
        }
    }

}