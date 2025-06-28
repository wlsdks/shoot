package com.stark.shoot.application.port.`in`.message.command

import com.stark.shoot.domain.chat.message.vo.MessageId
import com.stark.shoot.domain.chatroom.vo.ChatRoomId

/**
 * Command for getting messages from a chat room
 */
data class GetMessagesCommand(
    val roomId: ChatRoomId,
    val lastMessageId: MessageId?,
    val limit: Int
) {
    companion object {
        fun of(roomId: Long, lastMessageId: String?, limit: Int): GetMessagesCommand {
            return GetMessagesCommand(
                roomId = ChatRoomId.from(roomId),
                lastMessageId = lastMessageId?.let { MessageId.from(it) },
                limit = limit
            )
        }
    }
}