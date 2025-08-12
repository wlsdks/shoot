package com.stark.shoot.application.port.`in`.chatroom.group

import com.stark.shoot.application.port.`in`.chatroom.group.command.LeaveGroupChatCommand
import com.stark.shoot.application.port.`in`.chatroom.group.command.ManageGroupParticipantsCommand
import com.stark.shoot.application.port.`in`.chatroom.group.command.UpdateGroupChatTitleCommand
import com.stark.shoot.domain.chatroom.ChatRoom

/**
 * 그룹 채팅방 관리 Use Case
 */
interface ManageGroupChatUseCase {
    
    /**
     * 그룹 채팅방에 참여자를 추가하거나 제거합니다.
     */
    fun manageParticipants(command: ManageGroupParticipantsCommand): ChatRoom
    
    /**
     * 그룹 채팅방 제목을 변경합니다.
     */
    fun updateTitle(command: UpdateGroupChatTitleCommand): ChatRoom
    
    /**
     * 그룹 채팅방을 나갑니다.
     */
    fun leaveGroup(command: LeaveGroupChatCommand): ChatRoom?
}