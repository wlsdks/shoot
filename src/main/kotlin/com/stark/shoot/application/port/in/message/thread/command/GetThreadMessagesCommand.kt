package com.stark.shoot.application.port.`in`.message.thread.command

import com.stark.shoot.domain.chat.message.vo.MessageId

/**
 * Command for getting thread messages
 */
data class GetThreadMessagesCommand(
    val threadId: MessageId,
    val lastMessageId: MessageId?,
    val limit: Int
) {
    companion object {
        fun of(threadId: String, lastMessageId: String?, limit: Int): GetThreadMessagesCommand {
            return GetThreadMessagesCommand(
                threadId = MessageId.from(threadId),
                lastMessageId = lastMessageId?.let { MessageId.from(it) },
                limit = limit
            )
        }
    }
}