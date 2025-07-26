package com.stark.shoot.adapter.`in`.rest.mapper

import com.stark.shoot.adapter.`in`.rest.dto.chatroom.ChatRoomResponse
import com.stark.shoot.domain.chatroom.ChatRoom
import com.stark.shoot.domain.user.vo.UserId
import org.springframework.stereotype.Component

/**
 * 채팅방 응답 매퍼
 *
 * 채팅방 도메인 모델을 응답 DTO로 변환하는 매퍼입니다.
 * 순수 매핑 기능만 수행하며 도메인 로직은 포함하지 않습니다.
 */
@Component
class ChatRoomResponseMapper {

    /**
     * 채팅방 목록을 응답 DTO 목록으로 변환합니다.
     *
     * @param rooms 채팅방 목록
     * @param userId 사용자 ID
     * @param titles 채팅방 제목 맵 (roomId -> title)
     * @param lastMessages 마지막 메시지 맵 (roomId -> message)
     * @param timestamps 타임스탬프 맵 (roomId -> formatted timestamp)
     * @return 채팅방 응답 DTO 목록
     */
    fun toResponseList(
        rooms: List<ChatRoom>,
        userId: UserId,
        titles: Map<Long, String>,
        lastMessages: Map<Long, String>,
        timestamps: Map<Long, String>
    ): List<ChatRoomResponse> {
        return rooms.map { room ->
            toResponse(
                room, userId,
                titles[room.id?.value] ?: "채팅방",
                lastMessages[room.id?.value] ?: "메시지 없음",
                timestamps[room.id?.value] ?: ""
            )
        }
    }

    /**
     * 채팅방을 응답 DTO로 변환합니다.
     *
     * @param room 채팅방
     * @param userId 사용자 ID
     * @param title 채팅방 제목
     * @param lastMessage 마지막 메시지
     * @param timestamp 포맷된 타임스탬프
     * @return 채팅방 응답 DTO
     */
    fun toResponse(
        room: ChatRoom,
        userId: UserId,
        title: String,
        lastMessage: String,
        timestamp: String
    ): ChatRoomResponse {
        return ChatRoomResponse(
            roomId = room.id?.value ?: 0L,
            title = title,
            lastMessage = lastMessage,
            unreadMessages = 0, // 실제 구현시 읽지 않은 메시지 수 계산 로직 추가
            isPinned = room.pinnedParticipants.any { it.value == userId.value },
            timestamp = timestamp
        )
    }

}
