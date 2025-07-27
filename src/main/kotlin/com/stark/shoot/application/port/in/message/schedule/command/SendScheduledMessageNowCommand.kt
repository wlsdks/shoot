package com.stark.shoot.application.port.`in`.message.schedule.command

import com.stark.shoot.adapter.`in`.rest.dto.message.schedule.ScheduleMessageSendNowRequest
import com.stark.shoot.domain.user.vo.UserId
import org.springframework.security.core.Authentication

data class SendScheduledMessageNowCommand(
    val scheduledMessageId: String,
    val userId: UserId
) {

    companion object {
        fun of(
            scheduledMessageId: String,
            userId: Long
        ): SendScheduledMessageNowCommand {
            return SendScheduledMessageNowCommand(
                scheduledMessageId = scheduledMessageId,
                userId = UserId.from(userId)
            )
        }

        fun of(
            request: ScheduleMessageSendNowRequest,
            authentication: Authentication
        ): SendScheduledMessageNowCommand {
            val userId = authentication.name.toLong()
            return of(request.scheduledMessageId, userId)
        }
    }

}