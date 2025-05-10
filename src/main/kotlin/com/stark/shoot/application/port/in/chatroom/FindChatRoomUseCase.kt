package com.stark.shoot.application.port.`in`.chatroom

import com.stark.shoot.adapter.`in`.web.dto.chatroom.ChatRoomResponse

interface FindChatRoomUseCase {
    fun getChatRoomsForUser(userId: Long): List<ChatRoomResponse>

    /**
     * 두 사용자 간의 1:1 채팅방을 찾습니다.
     * 
     * @param userId1 첫 번째 사용자 ID
     * @param userId2 두 번째 사용자 ID
     * @return 두 사용자 간의 1:1 채팅방 응답 객체, 없으면 null
     */
    fun findDirectChatBetweenUsers(userId1: Long, userId2: Long): ChatRoomResponse?
}
