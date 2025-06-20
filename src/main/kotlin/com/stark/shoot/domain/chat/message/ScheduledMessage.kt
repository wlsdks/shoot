package com.stark.shoot.domain.chat.message

import com.stark.shoot.domain.chat.message.type.ScheduledMessageStatus
import com.stark.shoot.domain.chat.message.vo.ChatMessageMetadata
import com.stark.shoot.domain.chat.message.vo.MessageContent
import com.stark.shoot.domain.chat.message.vo.MessageId
import java.time.Instant

data class ScheduledMessage(
    val id: MessageId? = null,
    val roomId: Long,
    val senderId: Long,
    val content: MessageContent,
    val scheduledAt: Instant,
    val createdAt: Instant = Instant.now(),
    val status: ScheduledMessageStatus = ScheduledMessageStatus.PENDING,
    val metadata: ChatMessageMetadata = ChatMessageMetadata()
)
