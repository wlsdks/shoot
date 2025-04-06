package com.stark.shoot.application.service.chatroom

import com.stark.shoot.adapter.`in`.web.dto.chatroom.ChatRoomResponse
import com.stark.shoot.application.port.`in`.chatroom.ChatRoomSearchUseCase
import com.stark.shoot.application.port.out.chatroom.LoadChatRoomPort
import com.stark.shoot.domain.chat.room.ChatRoom
import com.stark.shoot.infrastructure.annotation.UseCase
import org.bson.types.ObjectId
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@UseCase
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
        userId: Long,
        query: String?,
        type: String?,
        unreadOnly: Boolean?
    ): List<ChatRoomResponse> {
        // 사용자가 참여한 채팅방 목록을 조회
        val chatRooms = loadChatRoomPort.findByParticipantId(userId)

        // Instant를 ZonedDateTime으로 변환하여 포맷 (예: "오후 3:15")
        val formatter = DateTimeFormatter.ofPattern("a h:mm")
        val zoneId = ZoneId.systemDefault()

        // ChatRoomResponse로 변환하여 반환
        return listOf()
    }

}