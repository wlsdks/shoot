package com.stark.shoot.domain.shared.event

import com.stark.shoot.domain.notification.type.NotificationType
import com.stark.shoot.domain.notification.type.SourceType
import com.stark.shoot.domain.shared.UserId
import java.time.Instant

/**
 * @property version Event schema version for MSA compatibility
 */
data class NotificationEvent(
    val version: String = "1.0",
    val id: String? = null,
    val timestamp: Instant = Instant.now(),
    val type: NotificationType,
    val title: String,
    val message: String,
    val sourceId: String,
    val sourceType: SourceType,
    val metadata: Map<String, Any> = emptyMap(),
    val recipients: List<UserId>,
    override val occurredOn: Long = System.currentTimeMillis()
) : DomainEvent {

}