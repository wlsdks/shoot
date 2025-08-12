package com.stark.shoot.application.port.out.chatroom

import com.stark.shoot.domain.chatroom.ChatRoom
import com.stark.shoot.domain.chatroom.vo.ChatRoomId
import com.stark.shoot.domain.user.vo.UserId

interface LoadChatRoomPort {
    fun findById(roomId: ChatRoomId): ChatRoom?
    fun findByParticipantId(participantId: UserId): List<ChatRoom>
    
    /**
     * 동일한 참여자로 구성된 그룹 채팅방 조회
     * 그룹 채팅방 중복 생성 방지용
     * TODO: 실제 구현 필요시 구현체에서 구현
     */
    fun findGroupChatByParticipants(participants: Set<UserId>): ChatRoom? = null
}