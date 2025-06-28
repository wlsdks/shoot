package com.stark.shoot.application.service.chatroom

import com.stark.shoot.adapter.`in`.web.dto.chatroom.ChatRoomResponse
import com.stark.shoot.adapter.`in`.web.mapper.ChatRoomResponseMapper
import com.stark.shoot.application.port.`in`.chatroom.FindChatRoomUseCase
import com.stark.shoot.application.port.`in`.chatroom.command.FindDirectChatCommand
import com.stark.shoot.application.port.`in`.chatroom.command.GetChatRoomsCommand
import com.stark.shoot.application.port.out.chatroom.ChatRoomQueryPort
import com.stark.shoot.domain.chatroom.service.ChatRoomDomainService
import com.stark.shoot.infrastructure.annotation.UseCase
import org.springframework.transaction.annotation.Transactional

@Transactional(readOnly = true)
@UseCase
class FindChatroomService(
    private val chatRoomQueryPort: ChatRoomQueryPort,
    private val chatRoomResponseMapper: ChatRoomResponseMapper,
    private val chatRoomDomainService: ChatRoomDomainService
) : FindChatRoomUseCase {

    /**
     * 사용자가 참여한 채팅방 목록을 조회합니다.
     *
     * @param command 채팅방 목록 조회 커맨드
     * @return ChatRoomResponse 채팅방 목록
     */
    override fun getChatRoomsForUser(command: GetChatRoomsCommand): List<ChatRoomResponse> {
        val userId = command.userId

        // 사용자가 참여한 채팅방 목록을 조회합니다.
        val chatRooms = chatRoomQueryPort.findByParticipantId(userId)

        // 채팅방 정보 준비
        val titles = chatRoomDomainService.prepareChatRoomTitles(chatRooms, userId)
        val lastMessages = chatRoomDomainService.prepareLastMessages(chatRooms)
        val timestamps = chatRoomDomainService.prepareTimestamps(chatRooms)

        // 채팅방 목록을 ChatRoomResponse로 변환하여 반환합니다.
        return chatRoomResponseMapper.toResponseList(chatRooms, userId, titles, lastMessages, timestamps)
    }

    /**
     * 두 사용자 간의 1:1 채팅방을 찾습니다.
     *
     * @param command 직접 채팅 찾기 커맨드
     * @return 두 사용자 간의 1:1 채팅방 응답 객체, 없으면 null
     */
    override fun findDirectChatBetweenUsers(command: FindDirectChatCommand): ChatRoomResponse? {
        val userId1 = command.userId1
        val userId2 = command.userId2

        // 첫 번째 사용자가 참여한 채팅방 목록을 조회합니다.
        val chatRooms = chatRoomQueryPort.findByParticipantId(userId1)

        // 두 사용자 간의 1:1 채팅방을 찾습니다. (도메인 객체의 정적 메서드 사용)
        val directChatRoom = chatRoomDomainService.findDirectChatBetween(chatRooms, userId1, userId2)

        // 채팅방이 없으면 null 반환
        return directChatRoom?.let {
            val title = it.createChatRoomTitle(userId1)
            val lastMessage = it.createLastMessageText()
            val timestamp = it.formatTimestamp()

            chatRoomResponseMapper.toResponse(it, userId1, title, lastMessage, timestamp)
        }
    }

}
