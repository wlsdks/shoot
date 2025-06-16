package com.stark.shoot.domain.service.chatroom

import com.stark.shoot.domain.chat.room.ChatRoom

/**
 * 채팅방 참여자 관리 도메인 서비스
 */
class ChatRoomParticipantDomainService {

    data class RemovalResult(
        val chatRoom: ChatRoom,
        val shouldDeleteRoom: Boolean
    )

    /**
     * 채팅방에 참여자를 추가합니다.
     * 단일 채팅방 엔티티의 addParticipant 로직을 래핑합니다.
     */
    fun addParticipant(chatRoom: ChatRoom, userId: Long): ChatRoom {
        return chatRoom.addParticipant(userId)
    }

    /**
     * 채팅방에서 참여자를 제거하고 삭제 필요 여부를 반환합니다.
     */
    fun removeParticipant(chatRoom: ChatRoom, userId: Long): RemovalResult {
        val updatedRoom = chatRoom.removeParticipant(userId)
        val shouldDelete = updatedRoom.shouldBeDeleted()
        return RemovalResult(updatedRoom, shouldDelete)
    }
}
