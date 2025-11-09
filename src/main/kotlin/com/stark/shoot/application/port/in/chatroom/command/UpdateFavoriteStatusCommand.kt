package com.stark.shoot.application.port.`in`.chatroom.command

import com.stark.shoot.adapter.`in`.rest.dto.chatroom.ChatRoomFavoriteRequest
import com.stark.shoot.domain.chatroom.vo.ChatRoomId
import com.stark.shoot.domain.shared.UserId

data class UpdateFavoriteStatusCommand(
    val roomId: ChatRoomId,
    val userId: UserId,
    val isFavorite: Boolean
) {

    companion object {
        fun of(request: ChatRoomFavoriteRequest): UpdateFavoriteStatusCommand {
            return UpdateFavoriteStatusCommand(
                roomId = ChatRoomId.from(request.roomId),
                userId = UserId.from(request.userId),
                isFavorite = request.isFavorite
            )
        }
    }

}