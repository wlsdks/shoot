package com.stark.shoot.domain.shared.event

import com.stark.shoot.domain.chat.message.vo.MessageId
import com.stark.shoot.domain.chat.vo.ChatRoomId
import com.stark.shoot.domain.shared.UserId
import java.time.Instant

/**
 * @property version Event schema version for MSA compatibility
 */
data class MentionEvent(
    val version: String = "1.0",
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