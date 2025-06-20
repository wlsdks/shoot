package com.stark.shoot.domain.event

import com.stark.shoot.domain.notification.type.NotificationType
import com.stark.shoot.domain.notification.type.SourceType
import com.stark.shoot.domain.user.vo.UserId
import com.stark.shoot.domain.chatroom.vo.ChatRoomId
import java.time.Instant

class NewMessageEvent(
    id: String? = null,
    val roomId: ChatRoomId,
    val senderId: UserId,
    val senderName: String,
    val messageContent: String,
    val recipientIds: Set<UserId>,
    timestamp: Instant = Instant.now(),
    metadata: Map<String, Any> = emptyMap()
) : NotificationEvent(
    id = id,
    timestamp = timestamp,
    type = NotificationType.NEW_MESSAGE,
    sourceId = roomId.value.toString(),
    sourceType = SourceType.CHAT_ROOM,
    metadata = metadata
) {

    override fun getRecipients(): Set<UserId> = recipientIds

    override fun getTitle(): String = "New message from $senderName"

    override fun getMessage(): String = messageContent.take(100).let {
        if (it.length < messageContent.length) "$it..." else it
    }

}
