package com.stark.shoot.application.port.`in`.chatroom.command

import com.stark.shoot.domain.chatroom.vo.ChatRoomId
import com.stark.shoot.domain.chatroom.vo.ChatRoomTitle

/**
 * Command for updating the title of a chat room
 */
data class UpdateTitleCommand(
    val roomId: ChatRoomId,
    val title: ChatRoomTitle
) {
    companion object {
        fun of(roomId: Long, title: String): UpdateTitleCommand {
            return UpdateTitleCommand(
                roomId = ChatRoomId.from(roomId),
                title = ChatRoomTitle.from(title)
            )
        }
    }
}