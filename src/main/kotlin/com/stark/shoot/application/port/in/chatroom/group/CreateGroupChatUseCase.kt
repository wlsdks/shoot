package com.stark.shoot.application.port.`in`.chatroom.group

import com.stark.shoot.application.port.`in`.chatroom.group.command.CreateGroupChatCommand
import com.stark.shoot.domain.chatroom.ChatRoom

/**
 * 그룹 채팅방 생성 Use Case
 */
interface CreateGroupChatUseCase {
    
    /**
     * 그룹 채팅방을 생성합니다.
     * 
     * @param command 그룹 채팅방 생성 명령
     * @return 생성된 채팅방
     */
    fun createGroupChat(command: CreateGroupChatCommand): ChatRoom
}