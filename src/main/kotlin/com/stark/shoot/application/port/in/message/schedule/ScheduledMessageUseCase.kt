package com.stark.shoot.application.port.`in`.message.schedule

import com.stark.shoot.adapter.`in`.web.dto.message.schedule.ScheduledMessageResponseDto
import com.stark.shoot.application.port.`in`.message.schedule.command.*

interface ScheduledMessageUseCase {
    fun scheduleMessage(command: ScheduleMessageCommand): ScheduledMessageResponseDto
    fun cancelScheduledMessage(command: CancelScheduledMessageCommand): ScheduledMessageResponseDto
    fun updateScheduledMessage(command: UpdateScheduledMessageCommand): ScheduledMessageResponseDto
    fun getScheduledMessagesByUser(command: GetScheduledMessagesCommand): List<ScheduledMessageResponseDto>
    fun sendScheduledMessageNow(command: SendScheduledMessageNowCommand): ScheduledMessageResponseDto

}
