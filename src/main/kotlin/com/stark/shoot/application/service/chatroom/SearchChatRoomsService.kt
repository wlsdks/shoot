package com.stark.shoot.application.service.chatroom

import com.stark.shoot.adapter.`in`.web.dto.chatroom.ChatRoomResponse
import com.stark.shoot.application.port.`in`.chatroom.SearchChatRoomsUseCase
import com.stark.shoot.application.port.out.chatroom.LoadChatRoomPort
import com.stark.shoot.domain.chat.room.ChatRoom
import com.stark.shoot.infrastructure.annotation.UseCase
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@UseCase
class SearchChatRoomsService(
    private val loadChatRoomPort: LoadChatRoomPort
) : SearchChatRoomsUseCase {

    // 타임스탬프 포맷터 (예: "오후 3:15")
    private val formatter = DateTimeFormatter.ofPattern("a h:mm")

    /**
     * 채팅방 검색
     *
     * @param userId 사용자 ID
     * @param query 검색어
     * @param type 채팅방 타입
     * @param unreadOnly 읽지 않은 메시지만
     * @return 채팅방 검색 결과 목록
     */
    override fun searchChatRooms(
        userId: Long,
        query: String?,
        type: String?,
        unreadOnly: Boolean?
    ): List<ChatRoomResponse> {
        // 사용자가 참여한 채팅방 목록을 조회
        val chatRooms = loadChatRoomPort.findByParticipantId(userId)

        // 필터링된 채팅방 목록을 반환
        val filteredRooms = processFiltering(chatRooms, query, type, unreadOnly, userId)

        // ChatRoomResponse로 변환하여 반환
        return mapToResponses(filteredRooms, userId)
    }

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
    private fun processFiltering(
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

    /**
     * 채팅방 목록을 ChatRoomResponse 목록으로 변환
     *
     * @param filteredRooms 필터링된 채팅방 목록
     * @param userId 사용자 ID
     * @return ChatRoomResponse 목록
     */
    private fun mapToResponses(
        filteredRooms: List<ChatRoom>,
        userId: Long
    ): List<ChatRoomResponse> {
        return filteredRooms.map { room ->
            ChatRoomResponse(
                roomId = room.id ?: 0L,
                title = createChatRoomTitle(room, userId),
                lastMessage = createLastMessageText(room),
                unreadMessages = 0, // 실제 구현시 읽지 않은 메시지 수 계산 로직 추가
                isPinned = room.pinnedParticipants.contains(userId),
                timestamp = room.lastActiveAt.atZone(ZoneId.systemDefault()).format(formatter)
            )
        }
    }

    /**
     * 채팅방 제목 생성
     *
     * @param room 채팅방
     * @param userId 사용자 ID
     * @return 채팅방 제목
     */
    private fun createChatRoomTitle(room: ChatRoom, userId: Long): String {
        return if (room.type.name == "INDIVIDUAL") {
            // 1:1 채팅인 경우, 현재 사용자를 제외한 다른 참여자의 정보 가져오기
            val otherParticipantId = room.participants.find { it != userId }
            // 실제 구현시 다른 참여자의 닉네임 등을 가져와 표시
            room.title ?: "1:1 채팅방"
        } else {
            // 그룹 채팅인 경우
            room.title ?: "그룹 채팅방"
        }
    }

    /**
     * 마지막 메시지 텍스트 생성
     *
     * @param room 채팅방
     * @return 마지막 메시지 텍스트
     */
    private fun createLastMessageText(room: ChatRoom): String {
        return room.lastMessageId?.let {
            // 실제 구현시 메시지 조회 로직 필요
            "최근 메시지"
        } ?: "메시지가 없습니다"
    }

}