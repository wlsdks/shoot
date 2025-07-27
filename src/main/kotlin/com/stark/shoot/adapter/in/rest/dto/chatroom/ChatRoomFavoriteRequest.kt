package com.stark.shoot.adapter.`in`.rest.dto.chatroom

data class ChatRoomFavoriteRequest(
    val roomId: Long,
    val userId: Long,
    val isFavorite: Boolean
) {
}