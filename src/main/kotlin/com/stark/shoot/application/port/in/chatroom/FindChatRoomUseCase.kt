package com.stark.shoot.application.port.`in`.chatroom

import com.stark.shoot.adapter.`in`.web.dto.chatroom.ChatRoomResponse
import com.stark.shoot.domain.user.vo.UserId

interface FindChatRoomUseCase {
    fun getChatRoomsForUser(userId: UserId): List<ChatRoomResponse>

    /**
     * 두 사용자 간의 1:1 채팅방을 찾습니다.
     *
     * @param userId1 첫 번째 사용자 ID
     * @param userId2 두 번째 사용자 ID
     * @return 두 사용자 간의 1:1 채팅방 응답 객체, 없으면 null
     */
    fun findDirectChatBetweenUsers(userId1: UserId, userId2: UserId): ChatRoomResponse?
}
