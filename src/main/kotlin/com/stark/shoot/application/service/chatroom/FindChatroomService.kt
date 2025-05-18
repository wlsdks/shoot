package com.stark.shoot.application.service.chatroom

import com.stark.shoot.adapter.`in`.web.dto.chatroom.ChatRoomResponse
import com.stark.shoot.application.port.`in`.chatroom.FindChatRoomUseCase
import com.stark.shoot.application.port.out.chatroom.LoadChatRoomPort
import com.stark.shoot.domain.chat.room.ChatRoom
import com.stark.shoot.domain.chat.room.service.ChatRoomDomainService
import com.stark.shoot.adapter.`in`.web.mapper.ChatRoomResponseMapper
import com.stark.shoot.infrastructure.annotation.UseCase
import org.springframework.transaction.annotation.Transactional

@Transactional(readOnly = true)
@UseCase
class FindChatroomService(
    private val loadChatRoomPort: LoadChatRoomPort,
    private val chatRoomResponseMapper: ChatRoomResponseMapper,
    private val chatRoomDomainService: ChatRoomDomainService
) : FindChatRoomUseCase {

    /**
     * 사용자가 참여한 채팅방 목록을 조회합니다.
     *
     * @param userId 사용자 ID
     * @return ChatRoomResponse 채팅방 목록
     */
    override fun getChatRoomsForUser(
        userId: Long
    ): List<ChatRoomResponse> {
        // 사용자가 참여한 채팅방 목록을 조회합니다.
        val chatRooms = loadChatRoomPort.findByParticipantId(userId)

        // 채팅방 정보 준비
        val titles = prepareChatRoomTitles(chatRooms, userId)
        val lastMessages = prepareLastMessages(chatRooms)
        val timestamps = prepareTimestamps(chatRooms)

        // 채팅방 목록을 ChatRoomResponse로 변환하여 반환합니다.
        return chatRoomResponseMapper.toResponseList(chatRooms, userId, titles, lastMessages, timestamps)
    }

    /**
     * 두 사용자 간의 1:1 채팅방을 찾습니다.
     *
     * @param userId1 첫 번째 사용자 ID
     * @param userId2 두 번째 사용자 ID
     * @return 두 사용자 간의 1:1 채팅방 응답 객체, 없으면 null
     */
    override fun findDirectChatBetweenUsers(
        userId1: Long,
        userId2: Long
    ): ChatRoomResponse? {
        // 첫 번째 사용자가 참여한 채팅방 목록을 조회합니다.
        val chatRooms = loadChatRoomPort.findByParticipantId(userId1)

        // 두 사용자 간의 1:1 채팅방을 찾습니다. (도메인 객체의 정적 메서드 사용)
        val directChatRoom = ChatRoom.findDirectChatBetween(chatRooms, userId1, userId2)

        // 채팅방이 없으면 null 반환
        return directChatRoom?.let { 
            val title = it.createChatRoomTitle(userId1)
            val lastMessage = it.createLastMessageText()
            val timestamp = it.formatTimestamp()

            chatRoomResponseMapper.toResponse(it, userId1, title, lastMessage, timestamp) 
        }
    }

    /**
     * 채팅방 제목 맵 준비
     */
    private fun prepareChatRoomTitles(chatRooms: List<ChatRoom>, userId: Long): Map<Long, String> {
        return chatRooms.associate { room ->
            val roomId = room.id ?: 0L
            val title = room.createChatRoomTitle(userId)
            roomId to title
        }
    }

    /**
     * 마지막 메시지 맵 준비
     */
    private fun prepareLastMessages(chatRooms: List<ChatRoom>): Map<Long, String> {
        return chatRooms.associate { room ->
            val roomId = room.id ?: 0L
            val lastMessage = room.createLastMessageText()
            roomId to lastMessage
        }
    }

    /**
     * 타임스탬프 맵 준비
     */
    private fun prepareTimestamps(chatRooms: List<ChatRoom>): Map<Long, String> {
        return chatRooms.associate { room ->
            val roomId = room.id ?: 0L
            val timestamp = room.formatTimestamp()
            roomId to timestamp
        }
    }
}
