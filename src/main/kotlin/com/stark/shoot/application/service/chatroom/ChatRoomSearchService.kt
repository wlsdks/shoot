package com.stark.shoot.application.service.chatroom

import com.stark.shoot.adapter.`in`.rest.dto.chatroom.ChatRoomResponse
import com.stark.shoot.infrastructure.mapper.ChatRoomResponseMapper
import com.stark.shoot.application.port.`in`.chatroom.ChatRoomSearchUseCase
import com.stark.shoot.application.port.`in`.chatroom.command.SearchChatRoomsCommand
import com.stark.shoot.application.port.out.chatroom.ChatRoomQueryPort
import com.stark.shoot.domain.chatroom.service.ChatRoomDomainService
import com.stark.shoot.infrastructure.annotation.UseCase

@UseCase
class ChatRoomSearchService(
    private val chatRoomQueryPort: ChatRoomQueryPort,
    private val chatRoomDomainService: ChatRoomDomainService,
    private val chatRoomResponseMapper: ChatRoomResponseMapper,
) : ChatRoomSearchUseCase {

    /**
     * 채팅방 검색
     *
     * @param command 채팅방 검색 커맨드
     * @return ChatRoomResponse 채팅방 목록
     */
    override fun searchChatRooms(command: SearchChatRoomsCommand): List<ChatRoomResponse> {
        val userId = command.userId
        val query = command.query
        val type = command.type
        val unreadOnly = command.unreadOnly

        // 사용자가 참여한 채팅방 목록을 조회
        val chatRooms = chatRoomQueryPort.findByParticipantId(userId)

        // 필터링된 채팅방 목록
        val filteredRooms = chatRoomDomainService.filterChatRooms(chatRooms, query, type, unreadOnly)

        // 채팅방 정보 준비
        val titles = chatRoomDomainService.prepareChatRoomTitles(filteredRooms, userId)
        val lastMessages = chatRoomDomainService.prepareLastMessages(filteredRooms)
        val timestamps = chatRoomDomainService.prepareTimestamps(filteredRooms)

        // ChatRoomResponse로 변환하여 반환
        return chatRoomResponseMapper.toResponseList(filteredRooms, userId, titles, lastMessages, timestamps)
    }
}
