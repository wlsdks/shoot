package com.stark.shoot.application.port.`in`.chatroom.command

import com.stark.shoot.domain.chatroom.vo.ChatRoomAnnouncement
import com.stark.shoot.domain.chatroom.vo.ChatRoomId

/**
 * Command for updating the announcement of a chat room
 */
data class UpdateAnnouncementCommand(
    val roomId: ChatRoomId,
    val announcement: ChatRoomAnnouncement?
) {
    companion object {
        fun of(roomId: Long, announcement: String?): UpdateAnnouncementCommand {
            return UpdateAnnouncementCommand(
                roomId = ChatRoomId.from(roomId),
                announcement = announcement?.let { ChatRoomAnnouncement.from(it) }
            )
        }
    }
}