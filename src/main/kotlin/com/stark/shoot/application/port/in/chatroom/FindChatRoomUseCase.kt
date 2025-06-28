package com.stark.shoot.application.port.`in`.chatroom

import com.stark.shoot.adapter.`in`.web.dto.chatroom.ChatRoomResponse
import com.stark.shoot.application.port.`in`.chatroom.command.FindDirectChatCommand
import com.stark.shoot.application.port.`in`.chatroom.command.GetChatRoomsCommand

interface FindChatRoomUseCase {
    /**
     * 사용자가 참여한 채팅방 목록을 조회합니다.
     *
     * @param command 채팅방 목록 조회 커맨드
     * @return ChatRoomResponse 채팅방 목록
     */
    fun getChatRoomsForUser(command: GetChatRoomsCommand): List<ChatRoomResponse>

    /**
     * 두 사용자 간의 1:1 채팅방을 찾습니다.
     *
     * @param command 직접 채팅 찾기 커맨드
     * @return 두 사용자 간의 1:1 채팅방 응답 객체, 없으면 null
     */
    fun findDirectChatBetweenUsers(command: FindDirectChatCommand): ChatRoomResponse?
}
