package com.stark.shoot.application.port.out.chatroom

/**
 * 채팅방 삭제를 위한 포트
 */
interface DeleteChatRoomPort {
    /**
     * 채팅방 ID로 채팅방 삭제
     *
     * @param roomId 채팅방 ID
     * @return 삭제 성공 여부
     */
    fun deleteById(roomId: Long): Boolean
}