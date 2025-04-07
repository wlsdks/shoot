package com.stark.shoot.application.port.out.message

import com.stark.shoot.domain.chat.message.ScheduledMessage
import org.bson.types.ObjectId
import java.time.Instant

interface ScheduledMessagePort {
    fun saveScheduledMessage(scheduledMessage: ScheduledMessage): ScheduledMessage
    fun findById(id: ObjectId): ScheduledMessage?
    fun findByUserId(userId: Long, roomId: Long? = null): List<ScheduledMessage>
    fun findPendingMessagesBeforeTime(time: Instant): List<ScheduledMessage>
}