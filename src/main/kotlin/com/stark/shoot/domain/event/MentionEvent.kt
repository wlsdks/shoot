package com.stark.shoot.domain.event

import com.stark.shoot.domain.chat.message.vo.MessageId
import com.stark.shoot.domain.chatroom.vo.ChatRoomId
import com.stark.shoot.domain.notification.type.NotificationType
import com.stark.shoot.domain.notification.type.SourceType
import com.stark.shoot.domain.user.vo.UserId
import java.time.Instant

class MentionEvent(
    id: String? = null,
    val roomId: ChatRoomId,
    val messageId: MessageId,
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
    sourceId = messageId.value,
    sourceType = SourceType.CHAT,
    metadata = metadata
) {

    override fun getRecipients(): Set<UserId> = mentionedUserIds

    override fun getTitle(): String = "$senderName mentioned you"

    override fun getMessage(): String = messageContent.take(100).let {
        if (it.length < messageContent.length) "$it..." else it
    }

}