package com.stark.shoot.application.service.chatroom

import com.stark.shoot.adapter.`in`.web.dto.chatroom.ChatRoomResponse
import com.stark.shoot.adapter.out.persistence.postgres.entity.enumerate.ChatRoomType
import com.stark.shoot.application.port.`in`.chatroom.GetChatRoomsForUserUseCase
import com.stark.shoot.application.port.out.chatroom.LoadChatRoomPort
import com.stark.shoot.domain.chat.room.ChatRoom
import com.stark.shoot.infrastructure.annotation.UseCase
import org.springframework.transaction.annotation.Transactional
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Transactional(readOnly = true)
@UseCase
class GetChatroomService(
    private val loadChatRoomPort: LoadChatRoomPort
) : GetChatRoomsForUserUseCase {

    // 타임스탬프 포맷터 (예: "오후 3:15")
    private val formatter = DateTimeFormatter.ofPattern("a h:mm")

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

        // 채팅방을 정렬합니다.
        val sortedRooms = processChatRoomSort(chatRooms, userId)

        // 정렬된 채팅방 목록을 ChatRoomResponse로 변환합니다.
        return sortedRooms.map { room -> mapToResponse(room, userId) }
    }

    /**
     * 채팅방 정렬
     *
     * @param chatRooms 채팅방 목록
     * @param userId 사용자 ID
     * @return List<ChatRoom> 정렬된 채팅방 목록
     */
    private fun processChatRoomSort(
        chatRooms: List<ChatRoom>,
        userId: Long
    ): List<ChatRoom> {
        // 고정된 채팅방이 먼저 나오도록 정렬
        return chatRooms.sortedWith(
            compareByDescending<ChatRoom> {
                // 사용자가 채팅방을 고정했으면 우선 표시
                it.pinnedParticipants.contains(userId)
            }.thenByDescending {
                // 마지막 활동 시간순으로 정렬
                it.lastActiveAt
            }
        )
    }

    /**
     * ChatRoom을 ChatRoomResponse로 변환합니다.
     *
     * @param room 채팅방
     * @param userId 사용자 ID
     * @return ChatRoomResponse
     */
    private fun mapToResponse(
        room: ChatRoom,
        userId: Long
    ): ChatRoomResponse {
        // 채팅방 제목 생성
        val roomTitle = createChatRoomTitle(room, userId)

        // 마지막 메시지 텍스트 생성
        val lastMessageText = createLastMessageText(room)

        // ChatRoomResponse로 변환
        return ChatRoomResponse(
            roomId = room.id ?: 0L,
            title = roomTitle,
            lastMessage = lastMessageText,
            unreadMessages = 0, // 추후 구현
            isPinned = room.pinnedParticipants.contains(userId),
            timestamp = room.lastActiveAt.atZone(ZoneId.systemDefault()).format(formatter)
        )
    }

    /**
     * 채팅방 제목 생성
     *
     * @param room 채팅방
     * @param userId 사용자 ID
     * @return String 채팅방 제목
     */
    private fun createChatRoomTitle(
        room: ChatRoom,
        userId: Long
    ): String {
        return if (ChatRoomType.INDIVIDUAL == room.type) {
            // 1:1 채팅인 경우, 현재 사용자를 제외한 다른 참여자의 ID를 찾습니다.
            val otherParticipantId = room.participants.find { it != userId }

            // 해당 참여자의 정보를 조회하여 표시 (실제 구현시 사용자 정보 서비스 활용)
            // 여기서는 기본값으로 title 또는 "채팅방" 반환
            room.title ?: "채팅방"
        } else {
            // 그룹 채팅인 경우 채팅방 제목 또는 기본값 사용
            room.title ?: "그룹 채팅방"
        }
    }

    /**
     * 마지막 메시지 텍스트 생성
     *
     * @param room 채팅방
     * @return String 마지막 메시지 텍스트
     */
    private fun createLastMessageText(
        room: ChatRoom
    ): String {
        return if (room.lastMessageId != null) {
            try {
                // 실제 구현에서는 메시지 ID로 메시지 내용 조회
                // 여기서는 간단히 "최근 메시지" 반환
                "최근 메시지"
            } catch (e: Exception) {
                "메시지 조회 오류"
            }
        } else {
            "최근 메시지가 없습니다."
        }
    }

}