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
    var content: MessageContent,
    var scheduledAt: Instant,
    var createdAt: Instant = Instant.now(),
    var status: ScheduledMessageStatus = ScheduledMessageStatus.PENDING,
    var metadata: ChatMessageMetadata = ChatMessageMetadata()
)
