package com.stark.shoot.application.port.`in`.chatroom.command

import com.stark.shoot.domain.chatroom.vo.ChatRoomId
import com.stark.shoot.domain.user.vo.UserId

/**
 * Command for updating the favorite status of a chat room for a user
 */
data class UpdateFavoriteStatusCommand(
    val roomId: ChatRoomId,
    val userId: UserId,
    val isFavorite: Boolean
) {
    companion object {
        fun of(roomId: Long, userId: Long, isFavorite: Boolean): UpdateFavoriteStatusCommand {
            return UpdateFavoriteStatusCommand(
                roomId = ChatRoomId.from(roomId),
                userId = UserId.from(userId),
                isFavorite = isFavorite
            )
        }
    }
}