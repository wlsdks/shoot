package com.stark.shoot.domain.event

import com.stark.shoot.domain.notification.type.NotificationType
import com.stark.shoot.domain.notification.type.SourceType
import com.stark.shoot.domain.user.vo.UserId
import java.time.Instant

data class NotificationEvent(
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