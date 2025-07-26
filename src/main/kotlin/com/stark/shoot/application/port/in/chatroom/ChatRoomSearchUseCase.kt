package com.stark.shoot.application.port.`in`.chatroom

import com.stark.shoot.adapter.`in`.rest.dto.chatroom.ChatRoomResponse
import com.stark.shoot.application.port.`in`.chatroom.command.SearchChatRoomsCommand

interface ChatRoomSearchUseCase {
    /**
     * 채팅방을 검색합니다.
     *
     * @param command 채팅방 검색 커맨드
     * @return 검색된 채팅방 목록
     */
    fun searchChatRooms(command: SearchChatRoomsCommand): List<ChatRoomResponse>
}
