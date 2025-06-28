package com.stark.shoot.application.port.`in`.message.command

import com.stark.shoot.domain.chat.message.vo.MessageId
import com.stark.shoot.domain.chatroom.vo.ChatRoomId
import com.stark.shoot.domain.user.vo.UserId

/**
 * Command for forwarding a message to a chat room
 */
data class ForwardMessageCommand(
    val originalMessageId: MessageId,
    val targetRoomId: ChatRoomId,
    val forwardingUserId: UserId
) {
    companion object {
        fun of(originalMessageId: String, targetRoomId: Long, forwardingUserId: Long): ForwardMessageCommand {
            return ForwardMessageCommand(
                originalMessageId = MessageId.from(originalMessageId),
                targetRoomId = ChatRoomId.from(targetRoomId),
                forwardingUserId = UserId.from(forwardingUserId)
            )
        }
    }
}