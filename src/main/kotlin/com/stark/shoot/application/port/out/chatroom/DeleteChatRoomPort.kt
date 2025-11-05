package com.stark.shoot.application.port.out.chatroom

import com.stark.shoot.domain.chatroom.vo.ChatRoomId
import com.stark.shoot.infrastructure.exception.web.MongoOperationException
import com.stark.shoot.infrastructure.exception.web.ResourceNotFoundException

/**
 * 채팅방 삭제를 위한 포트
 */
interface DeleteChatRoomPort {
    /**
     * 채팅방 ID로 채팅방 삭제
     *
     * @param roomId 채팅방 ID
     * @throws ResourceNotFoundException 채팅방을 찾을 수 없는 경우
     * @throws MongoOperationException 데이터베이스 작업 실패 시
     */
    fun deleteById(roomId: ChatRoomId) : Boolean
}
