package com.stark.shoot.domain.chat.room.service

import com.stark.shoot.domain.chat.room.ChatRoom
import org.springframework.stereotype.Service

/**
 * 채팅방 도메인 서비스
 *
 * 채팅방 도메인 모델에 대한 비즈니스 로직을 처리하는 서비스입니다.
 * 단일 채팅방 엔티티로 처리할 수 없는 복잡한 도메인 로직을 담당합니다.
 */
@Service
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
        unreadOnly: Boolean?,
        userId: Long
    ): List<ChatRoom> {
        return chatRooms.filter { room ->
            // 검색어 필터링 (제목에 검색어 포함 여부)
            val matchesQuery = query.isNullOrBlank() ||
                    (room.title?.contains(query, ignoreCase = true) ?: false)

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

}