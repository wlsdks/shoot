package com.stark.shoot.application.port.`in`.message.command

import com.stark.shoot.adapter.`in`.rest.dto.message.forward.ForwardMessageToUserRequest
import com.stark.shoot.domain.chat.message.vo.MessageId
import com.stark.shoot.domain.shared.UserId

data class ForwardMessageToUserCommand(
    val originalMessageId: MessageId,
    val targetUserId: UserId,
    val forwardingUserId: UserId
) {

    companion object {
        fun of(request: ForwardMessageToUserRequest): ForwardMessageToUserCommand {
            return ForwardMessageToUserCommand(
                originalMessageId = MessageId.from(request.originalMessageId),
                targetUserId = UserId.from(request.targetUserId),
                forwardingUserId = UserId.from(request.forwardingUserId)
            )
        }
    }

}