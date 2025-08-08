package com.stark.shoot.application.port.`in`.message.thread.command

import com.stark.shoot.adapter.`in`.socket.dto.ThreadDetailRequestDto
import com.stark.shoot.domain.chat.message.vo.MessageId

/**
 * Command for getting thread detail
 */
data class GetThreadDetailCommand(
    val threadId: MessageId,
    val lastMessageId: MessageId?,
    val limit: Int,
    val userId: Long
) {
    companion object {
        /**
         * Factory method to create a GetThreadDetailCommand from a ThreadDetailRequestDto
         *
         * @param request The thread detail request
         * @return A new GetThreadDetailCommand
         */
        fun of(request: ThreadDetailRequestDto): GetThreadDetailCommand {
            return GetThreadDetailCommand(
                threadId = MessageId.from(request.threadId),
                lastMessageId = request.lastMessageId?.let { MessageId.from(it) },
                limit = request.limit,
                userId = request.userId
            )
        }
    }
}