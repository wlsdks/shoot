package com.stark.shoot.domain.chatroom.service

import com.stark.shoot.domain.chatroom.ChatRoom
import com.stark.shoot.domain.user.vo.UserId

/**
 * 채팅방 도메인 서비스
 *
 * 채팅방 도메인 모델에 대한 비즈니스 로직을 처리하는 서비스입니다.
 * 단일 채팅방 엔티티로 처리할 수 없는 복잡한 도메인 로직을 담당합니다.
 */
class ChatRoomDomainService {

    /**
     * 채팅방 필터링
     *
     * @param chatRooms 채팅방 목록
     * @param query 쿼리 검색어
     * @param type 채팅방 타입
     * @param unreadOnly 읽지 않은 메시지만
     * @param userId 사용자 ID
     * @return 필터링된 채팅방 목록
     */
    fun filterChatRooms(
        chatRooms: List<ChatRoom>,
        query: String?,
        type: String?,
        unreadOnly: Boolean?
    ): List<ChatRoom> {
        return chatRooms.filter { room ->
            // 검색어 필터링 (제목에 검색어 포함 여부)
            val matchesQuery = query.isNullOrBlank() ||
                    (room.title?.value?.contains(query, ignoreCase = true) ?: false)

            // 채팅방 타입 필터링
            val matchesType = type.isNullOrBlank() ||
                    room.type.name.equals(type, ignoreCase = true)

            // 읽지 않은 메시지 필터링 (현재는 기능 미구현으로 항상 true)
            // 실제 구현시 읽지 않은 메시지 수 확인 로직 추가 필요
            val matchesUnread = unreadOnly != true

            // 모든 조건을 만족하는 경우만 반환
            matchesQuery && matchesType && matchesUnread
        }
    }

    /**
     * 기존 채팅방 목록에서 두 사용자 간의 1:1 채팅방 찾기
     *
     * @param chatRooms 검색할 채팅방 목록
     * @param userId 사용자 ID
     * @param friendId 친구 ID
     * @return 찾은 채팅방 또는 null
     */
    fun findDirectChatBetween(
        chatRooms: List<ChatRoom>,
        userId: UserId,
        friendId: UserId
    ): ChatRoom? {
        return chatRooms.firstOrNull { chatRoom ->
            chatRoom.isDirectChatBetween(userId, friendId)
        }
    }

    /**
     * 채팅방 제목 맵 준비
     */
    fun prepareChatRoomTitles(
        chatRooms: List<ChatRoom>,
        userId: UserId
    ): Map<Long, String> {
        return chatRooms.associate { room ->
            val roomId = room.id?.value ?: 0L
            val title = room.createChatRoomTitle(userId)
            roomId to title
        }
    }

    /**
     * 마지막 메시지 맵 준비
     */
    fun prepareLastMessages(
        chatRooms: List<ChatRoom>
    ): Map<Long, String> {
        return chatRooms.associate { room ->
            val roomId = room.id?.value ?: 0L
            val lastMessage = room.createLastMessageText()
            roomId to lastMessage
        }
    }

    /**
     * 타임스탬프 맵 준비
     */
    fun prepareTimestamps(
        chatRooms: List<ChatRoom>
    ): Map<Long, String> {
        return chatRooms.associate { room ->
            val roomId = room.id?.value ?: 0L
            val timestamp = room.formatTimestamp()
            roomId to timestamp
        }
    }

}
