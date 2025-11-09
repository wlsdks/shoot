package com.stark.shoot.application.port.`in`.chatroom.command

import com.stark.shoot.domain.shared.UserId

/**
 * Command for searching chat rooms
 */
data class SearchChatRoomsCommand(
    val userId: UserId,
    val query: String?,
    val type: String?,
    val unreadOnly: Boolean?
) {
    companion object {
        fun of(userId: Long, query: String?, type: String?, unreadOnly: Boolean?): SearchChatRoomsCommand {
            return SearchChatRoomsCommand(
                userId = UserId.from(userId),
                query = query,
                type = type,
                unreadOnly = unreadOnly
            )
        }
    }
}