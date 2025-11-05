package com.stark.shoot.application.port.out.message

import com.stark.shoot.domain.chatroom.vo.ChatRoomId
import com.stark.shoot.domain.shared.UserId

interface MessageQueryPort : LoadMessagePort {

    /**
     * 특정 사용자의 특정 채팅방에서 안읽은 메시지 개수 조회
     * MongoDB count 쿼리를 사용하여 최적화된 성능 제공
     */
    fun countUnreadMessages(userId: UserId, roomId: ChatRoomId): Int

    /**
     * 여러 사용자의 특정 채팅방에서 안읽은 메시지 개수를 배치로 조회
     * N+1 쿼리 문제를 방지하기 위한 배치 쿼리
     *
     * @param userIds 조회할 사용자 ID 목록
     * @param roomId 채팅방 ID
     * @return userId -> 안읽은 메시지 개수 맵
     */
    fun countUnreadMessagesBatch(userIds: Set<UserId>, roomId: ChatRoomId): Map<UserId, Int>
}
