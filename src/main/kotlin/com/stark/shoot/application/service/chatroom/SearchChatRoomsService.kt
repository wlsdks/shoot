package com.stark.shoot.application.service.chatroom

import com.stark.shoot.adapter.`in`.web.dto.chatroom.ChatRoomResponse
import com.stark.shoot.application.port.`in`.chatroom.SearchChatRoomsUseCase
import com.stark.shoot.application.port.out.chatroom.LoadChatRoomPort
import com.stark.shoot.domain.chat.room.ChatRoom
import com.stark.shoot.domain.chat.room.service.ChatRoomDomainService
import com.stark.shoot.adapter.`in`.web.mapper.ChatRoomResponseMapper
import com.stark.shoot.infrastructure.annotation.UseCase

@UseCase
class SearchChatRoomsService(
    private val loadChatRoomPort: LoadChatRoomPort,
    private val chatRoomDomainService: ChatRoomDomainService,
    private val chatRoomResponseMapper: ChatRoomResponseMapper
) : SearchChatRoomsUseCase {

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

        // 채팅방 정보 준비
        val titles = prepareChatRoomTitles(filteredRooms, userId)
        val lastMessages = prepareLastMessages(filteredRooms)
        val timestamps = prepareTimestamps(filteredRooms)

        // ChatRoomResponse로 변환하여 반환
        return chatRoomResponseMapper.toResponseList(filteredRooms, userId, titles, lastMessages, timestamps)
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
        return chatRoomDomainService.filterChatRooms(chatRooms, query, type, unreadOnly, userId)
    }

    /**
     * 채팅방 제목 맵 준비
     */
    private fun prepareChatRoomTitles(
        chatRooms: List<ChatRoom>,
        userId: Long
    ): Map<Long, String> {
        return chatRooms.associate { room ->
            val roomId = room.id ?: 0L
            val title = room.createChatRoomTitle(userId)
            roomId to title
        }
    }

    /**
     * 마지막 메시지 맵 준비
     */
    private fun prepareLastMessages(
        chatRooms: List<ChatRoom>
    ): Map<Long, String> {
        return chatRooms.associate { room ->
            val roomId = room.id ?: 0L
            val lastMessage = room.createLastMessageText()
            roomId to lastMessage
        }
    }

    /**
     * 타임스탬프 맵 준비
     */
    private fun prepareTimestamps(
        chatRooms: List<ChatRoom>
    ): Map<Long, String> {
        return chatRooms.associate { room ->
            val roomId = room.id ?: 0L
            val timestamp = room.formatTimestamp()
            roomId to timestamp
        }
    }

}
