package com.stark.shoot.application.port.`in`.message.thread.command

import com.stark.shoot.domain.chat.message.vo.MessageId
import com.stark.shoot.domain.chatroom.vo.ChatRoomId

/**
 * Command for getting threads in a chat room
 */
data class GetThreadsCommand(
    val roomId: ChatRoomId,
    val lastThreadId: MessageId?,
    val limit: Int
) {
    companion object {
        fun of(roomId: ChatRoomId, lastThreadId: MessageId?, limit: Int): GetThreadsCommand {
            return GetThreadsCommand(roomId, lastThreadId, limit)
        }
        
        fun of(roomId: Long, lastThreadId: String?, limit: Int): GetThreadsCommand {
            return GetThreadsCommand(
                roomId = ChatRoomId.from(roomId),
                lastThreadId = lastThreadId?.let { MessageId.from(it) },
                limit = limit
            )
        }
    }
}