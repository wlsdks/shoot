package com.stark.shoot.domain.notification.event

import com.stark.shoot.domain.notification.NotificationType
import com.stark.shoot.domain.notification.SourceType
import java.time.Instant

abstract class NotificationEvent(
    val id: String? = null,
    val timestamp: Instant = Instant.now(),
    val type: NotificationType,
    val sourceId: String,
    val sourceType: SourceType,
    val metadata: Map<String, Any> = emptyMap()
) {

    abstract fun getRecipients(): Set<Long>

    abstract fun getTitle(): String

    abstract fun getMessage(): String

}