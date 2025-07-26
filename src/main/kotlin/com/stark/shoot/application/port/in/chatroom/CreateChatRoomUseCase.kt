package com.stark.shoot.application.port.`in`.chatroom

import com.stark.shoot.adapter.`in`.rest.dto.chatroom.ChatRoomResponse
import com.stark.shoot.application.port.`in`.chatroom.command.CreateDirectChatCommand

interface CreateChatRoomUseCase {
    /**
     * 1:1 채팅방 생성
     *
     * @param command 직접 채팅 생성 커맨드
     * @return ChatRoomResponse 생성된 채팅방 정보
     */
    fun createDirectChat(command: CreateDirectChatCommand): ChatRoomResponse
}
