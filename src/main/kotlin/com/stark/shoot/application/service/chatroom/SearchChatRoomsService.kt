package com.stark.shoot.application.service.chatroom

import com.stark.shoot.adapter.`in`.web.dto.chatroom.ChatRoomResponse
import com.stark.shoot.adapter.`in`.web.mapper.ChatRoomResponseMapper
import com.stark.shoot.application.port.`in`.chatroom.SearchChatRoomsUseCase
import com.stark.shoot.application.port.out.chatroom.ChatRoomQueryPort
import com.stark.shoot.domain.chatroom.service.ChatRoomDomainService
import com.stark.shoot.domain.user.vo.UserId
import com.stark.shoot.infrastructure.annotation.UseCase

@UseCase
class SearchChatRoomsService(
    private val chatRoomQueryPort: ChatRoomQueryPort,
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
        userId: UserId,
        query: String?,
        type: String?,
        unreadOnly: Boolean?
    ): List<ChatRoomResponse> {
        // 사용자가 참여한 채팅방 목록을 조회
        val chatRooms = chatRoomQueryPort.findByParticipantId(userId)

        // 필터링된 채팅방 목록을 반환
        val filteredRooms = chatRoomDomainService.filterChatRooms(chatRooms, query, type, unreadOnly)

        // 채팅방 정보 준비
        val titles = chatRoomDomainService.prepareChatRoomTitles(filteredRooms, userId)
        val lastMessages = chatRoomDomainService.prepareLastMessages(filteredRooms)
        val timestamps = chatRoomDomainService.prepareTimestamps(filteredRooms)

        // ChatRoomResponse로 변환하여 반환
        return chatRoomResponseMapper.toResponseList(filteredRooms, userId, titles, lastMessages, timestamps)
    }

}
