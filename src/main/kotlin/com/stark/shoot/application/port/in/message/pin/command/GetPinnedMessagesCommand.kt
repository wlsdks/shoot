package com.stark.shoot.application.port.`in`.message.pin.command

import com.stark.shoot.domain.chatroom.vo.ChatRoomId

/**
 * Command for getting pinned messages
 */
data class GetPinnedMessagesCommand(
    val roomId: ChatRoomId
) {
    companion object {
        /**
         * Factory method to create a GetPinnedMessagesCommand
         *
         * @param roomId The chat room ID
         * @return A new GetPinnedMessagesCommand
         */
        fun of(roomId: Long): GetPinnedMessagesCommand {
            return GetPinnedMessagesCommand(
                roomId = ChatRoomId.from(roomId)
            )
        }
    }
}