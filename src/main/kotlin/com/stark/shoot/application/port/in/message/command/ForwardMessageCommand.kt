package com.stark.shoot.application.port.`in`.message.command

import com.stark.shoot.adapter.`in`.rest.dto.message.forward.ForwardMessageToRoomRequest
import com.stark.shoot.domain.chat.message.vo.MessageId
import com.stark.shoot.domain.chatroom.vo.ChatRoomId
import com.stark.shoot.domain.shared.UserId

data class ForwardMessageCommand(
    val originalMessageId: MessageId,
    val targetRoomId: ChatRoomId,
    val forwardingUserId: UserId
) {

    companion object {
        fun of(request: ForwardMessageToRoomRequest): ForwardMessageCommand {
            return ForwardMessageCommand(
                originalMessageId = MessageId.from(request.originalMessageId),
                targetRoomId = ChatRoomId.from(request.targetRoomId),
                forwardingUserId = UserId.from(request.forwardingUserId)
            )
        }
    }

}