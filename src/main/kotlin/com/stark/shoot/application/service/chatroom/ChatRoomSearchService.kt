package com.stark.shoot.application.service.chatroom

import com.stark.shoot.adapter.`in`.web.dto.chatroom.ChatRoomResponse
import com.stark.shoot.application.port.`in`.chatroom.ChatRoomSearchUseCase
import com.stark.shoot.application.port.out.chatroom.LoadChatRoomPort
import org.bson.types.ObjectId
import org.springframework.stereotype.Service
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Service
class ChatRoomSearchService(
    private val loadChatRoomPort: LoadChatRoomPort,
) : ChatRoomSearchUseCase {

    /**
     * 채팅방 검색
     *
     * @param userId 사용자 ID
     * @param query 검색어
     * @param type 채팅방 타입
     * @param unreadOnly 읽지 않은 메시지만
     * @return ChatRoomResponse 채팅방 목록
     */
    override fun searchChatRooms(
        userId: ObjectId,
        query: String?,
        type: String?,
        unreadOnly: Boolean?
    ): List<ChatRoomResponse> {
        // 사용자가 참여한 채팅방 목록을 조회
        val chatRooms = loadChatRoomPort.findByParticipantId(userId)

        // 필터링된 채팅방 목록을 반환
        val filteredRooms = chatRooms.filter { room ->
            (query.isNullOrBlank() || (room.metadata.title?.contains(query, ignoreCase = true) ?: false)) &&
                    (type.isNullOrBlank() || room.metadata.type.name.equals(type, ignoreCase = true)) &&
                    (unreadOnly != true || (room.metadata.participantsMetadata[userId]?.unreadCount ?: 0) > 0)
        }

        // Instant를 ZonedDateTime으로 변환하여 포맷 (예: "오후 3:15")
        val formatter = DateTimeFormatter.ofPattern("a h:mm")
        val zoneId = ZoneId.systemDefault()

        // ChatRoomResponse로 변환하여 반환
        return filteredRooms.map { room ->
            ChatRoomResponse(
                roomId = room.id!!,
                title = room.metadata.title ?: "Untitled Room",
                lastMessage = room.lastMessageId,
                unreadMessages = room.metadata.participantsMetadata[userId]?.unreadCount ?: 0,
                isPinned = room.metadata.participantsMetadata[userId]?.isPinned ?: false,
                timestamp = formatter.format(room.lastActiveAt.atZone(zoneId))
            )
        }
    }

}