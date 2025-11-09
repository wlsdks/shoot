package com.stark.shoot.application.port.`in`.chatroom.command

import com.stark.shoot.domain.chatroom.vo.ChatRoomId
import com.stark.shoot.domain.shared.UserId

/**
 * Command for sending an update to a user about a chat room
 */
data class SendUpdateCommand(
    val userId: UserId,
    val roomId: ChatRoomId,
    val unreadCount: Int,
    val lastMessage: String?
) {
    companion object {
        fun of(
            userId: UserId,
            roomId: ChatRoomId,
            unreadCount: Int,
            lastMessage: String?
        ): SendUpdateCommand {
            return SendUpdateCommand(
                userId = userId,
                roomId = roomId,
                unreadCount = unreadCount,
                lastMessage = lastMessage
            )
        }
        
        fun of(
            userId: Long,
            roomId: Long,
            unreadCount: Int,
            lastMessage: String?
        ): SendUpdateCommand {
            return SendUpdateCommand(
                userId = UserId.from(userId),
                roomId = ChatRoomId.from(roomId),
                unreadCount = unreadCount,
                lastMessage = lastMessage
            )
        }
    }
}