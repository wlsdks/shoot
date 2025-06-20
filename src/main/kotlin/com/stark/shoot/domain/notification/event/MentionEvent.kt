package com.stark.shoot.domain.notification.event

import com.stark.shoot.domain.notification.NotificationType
import com.stark.shoot.domain.notification.SourceType
import com.stark.shoot.domain.common.vo.UserId
import java.time.Instant

class MentionEvent(
    id: String? = null,
    val roomId: Long,
    val messageId: String,
    val senderId: UserId,
    val senderName: String,
    val mentionedUserIds: Set<UserId>,
    val messageContent: String,
    timestamp: Instant = Instant.now(),
    metadata: Map<String, Any> = emptyMap()
) : NotificationEvent(
    id = id,
    timestamp = timestamp,
    type = NotificationType.MENTION,
    sourceId = messageId,
    sourceType = SourceType.CHAT,
    metadata = metadata
) {

    override fun getRecipients(): Set<UserId> = mentionedUserIds

    override fun getTitle(): String = "$senderName mentioned you"

    override fun getMessage(): String = messageContent.take(100).let {
        if (it.length < messageContent.length) "$it..." else it
    }

}