package com.stark.shoot.application.port.`in`.message.command

import com.stark.shoot.domain.chat.message.vo.MessageId
import com.stark.shoot.domain.user.vo.UserId

/**
 * Command for forwarding a message to a user
 */
data class ForwardMessageToUserCommand(
    val originalMessageId: MessageId,
    val targetUserId: UserId,
    val forwardingUserId: UserId
) {
    companion object {
        fun of(originalMessageId: String, targetUserId: Long, forwardingUserId: Long): ForwardMessageToUserCommand {
            return ForwardMessageToUserCommand(
                originalMessageId = MessageId.from(originalMessageId),
                targetUserId = UserId.from(targetUserId),
                forwardingUserId = UserId.from(forwardingUserId)
            )
        }
    }
}