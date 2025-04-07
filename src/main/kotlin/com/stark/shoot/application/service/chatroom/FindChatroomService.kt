package com.stark.shoot.application.service.chatroom

import com.stark.shoot.adapter.`in`.web.dto.chatroom.ChatRoomResponse
import com.stark.shoot.adapter.out.persistence.postgres.entity.enumerate.ChatRoomType
import com.stark.shoot.application.port.`in`.chatroom.FindChatRoomUseCase
import com.stark.shoot.application.port.out.chatroom.LoadChatRoomPort
import com.stark.shoot.domain.chat.room.ChatRoom
import com.stark.shoot.infrastructure.annotation.UseCase
import org.springframework.transaction.annotation.Transactional
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Transactional(readOnly = true)
@UseCase
class FindChatroomService(
    private val loadChatRoomPort: LoadChatRoomPort
) : FindChatRoomUseCase {

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

        // 채팅방 목록을 ChatRoomResponse로 변환하여 반환합니다.
        return chatRooms.map { room -> mapToResponse(room, userId) }
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
        // 채팅방 제목 결정
        val roomTitle = createChatRoomTitle(room, userId)

        // 마지막 메시지 가져오기
        val lastMessageText = createLastMessageText(room)

        // 해당 사용자의 채팅방 고정 여부 확인
        val isPinned = room.pinnedParticipants.contains(userId)

        // 읽지 않은 메시지 수 (실제 구현에서는 관련 로직 필요)
        val unreadCount = 0 // 향후 구현

        // ChatRoomResponse 객체 생성 및 반환
        return ChatRoomResponse(
            roomId = room.id ?: 0L,
            title = roomTitle,
            lastMessage = lastMessageText,
            unreadMessages = unreadCount,
            isPinned = isPinned,
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
            // 1:1 채팅인 경우, 상대방 사용자의 이름을 제목으로 설정 (participants 컬렉션에서 현재 사용자를 제외한 다른 참여자를 찾아 닉네임 표시)
            val otherParticipantId = room.participants.find { it != userId }
            if (otherParticipantId != null) {
                // 실제 구현에서는 사용자 정보 조회 서비스를 통해 닉네임 가져오기
                room.title ?: "1:1 채팅방"
            } else {
                room.title ?: "1:1 채팅방"
            }
        } else {
            // 그룹 채팅의 경우 정해진 제목 사용
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
                // 마지막 메시지 ID가 있는 경우, 해당 메시지 내용 조회
                // 실제 구현에서는 메시지 저장소에서 해당 ID의 메시지 조회
                "최근 메시지" // 실제 구현시 메시지 조회 후 내용 반환
            } catch (e: Exception) {
                "메시지 조회 실패"
            }
        } else {
            "최근 메시지가 없습니다."
        }
    }

}
