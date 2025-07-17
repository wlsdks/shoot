package com.stark.shoot.application.port.out.chatroom

import com.stark.shoot.domain.chatroom.vo.ChatRoomId
import com.stark.shoot.domain.event.ChatRoomUpdateEvent
import com.stark.shoot.domain.user.vo.UserId

/**
 * 채팅방 업데이트 정보를 사용자에게 전송하기 위한 포트
 */
interface SendChatRoomUpdatePort {
    fun sendUpdate(userId: UserId, roomId: ChatRoomId, update: ChatRoomUpdateEvent.Update)
}
