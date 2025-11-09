package com.stark.shoot.domain.chatroom.service

import com.stark.shoot.domain.chatroom.ChatRoom
import com.stark.shoot.domain.shared.UserId
import com.stark.shoot.domain.chatroom.constants.ChatRoomConstants
import org.springframework.stereotype.Service

/**
 * 채팅방 참여자 관리 도메인 서비스
 */
@Service
class ChatRoomParticipantDomainService(
    private val chatRoomConstants: ChatRoomConstants
) {

    data class RemovalResult(
        val chatRoom: ChatRoom,
        val shouldDeleteRoom: Boolean
    )

    /**
     * 채팅방에 참여자를 추가합니다.
     * 단일 채팅방 엔티티의 addParticipant 로직을 래핑합니다.
     */
    fun addParticipant(
        chatRoom: ChatRoom,
        userId: UserId
    ): ChatRoom {
        chatRoom.addParticipant(userId)
        return chatRoom
    }

    /**
     * 채팅방에서 참여자를 제거하고 삭제 필요 여부를 반환합니다.
     */
    fun removeParticipant(
        chatRoom: ChatRoom,
        userId: UserId
    ): RemovalResult {
        chatRoom.removeParticipant(userId)
        val shouldDelete = chatRoom.shouldBeDeleted()
        return RemovalResult(chatRoom, shouldDelete)
    }

    /**
     * 참여자 변경 정보를 적용합니다.
     *
     * DDD 개선: 즐겨찾기(핀) 기능은 ChatRoomFavorite Aggregate에서 관리
     *
     * @param chatRoom 대상 채팅방
     * @param changes 변경 정보
     * @return 업데이트된 채팅방
     */
    fun applyChanges(
        chatRoom: ChatRoom,
        changes: ChatRoom.ParticipantChanges
    ): ChatRoom {
        if (changes.participantsToAdd.isNotEmpty()) {
            chatRoom.addParticipants(changes.participantsToAdd)
        }

        if (changes.participantsToRemove.isNotEmpty()) {
            chatRoom.removeParticipants(changes.participantsToRemove)
        }

        return chatRoom
    }
}
