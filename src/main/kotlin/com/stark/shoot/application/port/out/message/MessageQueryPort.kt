package com.stark.shoot.application.port.out.message

import com.stark.shoot.domain.chatroom.vo.ChatRoomId
import com.stark.shoot.domain.user.vo.UserId

interface MessageQueryPort : LoadMessagePort {

    /**
     * 특정 사용자의 특정 채팅방에서 안읽은 메시지 개수 조회
     * MongoDB count 쿼리를 사용하여 최적화된 성능 제공
     */
    fun countUnreadMessages(userId: UserId, roomId: ChatRoomId): Int
}
