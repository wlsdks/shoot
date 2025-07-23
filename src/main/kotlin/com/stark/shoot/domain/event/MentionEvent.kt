package com.stark.shoot.domain.event

import com.stark.shoot.domain.chat.message.vo.MessageId
import com.stark.shoot.domain.chatroom.vo.ChatRoomId
import com.stark.shoot.domain.user.vo.UserId
import java.time.Instant

data class MentionEvent(
    val id: String? = null,
    val roomId: ChatRoomId,
    val messageId: MessageId,
    val senderId: UserId,
    val senderName: String,
    val mentionedUserIds: Set<UserId>,
    val messageContent: String,
    val timestamp: Instant = Instant.now(),
    val metadata: Map<String, Any> = emptyMap(),
    override val occurredOn: Long = System.currentTimeMillis()
) : DomainEvent {

}