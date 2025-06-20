package com.stark.shoot.adapter.`in`.web.dto.chatroom

import com.stark.shoot.domain.chat.room.ChatRoom
import com.stark.shoot.domain.chat.room.ChatRoomType
import com.stark.shoot.infrastructure.annotation.ApplicationDto
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@ApplicationDto
data class ChatRoomResponse(
    val roomId: Long,
    val title: String,  // 1:1 채팅인 경우, 상대방의 이름이나 채팅방 제목을 담습니다.
    val lastMessage: String?,
    val unreadMessages: Int,
    val isPinned: Boolean,
    val timestamp: String
) {
    companion object {
        // 타임스탬프 포맷 (예: "오후 3:15")
        private val formatter = DateTimeFormatter.ofPattern("a h:mm")

        /**
         * 특정 사용자의 관점에서 도메인 ChatRoom을 DTO로 변환합니다.
         *
         * 도메인 ChatRoom에는 metadata 대신 title, pinnedParticipants 등의 필드가 있으므로,
         * - unreadMessages는 별도 정보가 없으므로 기본값 0으로 처리합니다.
         * - isPinned는 현재 사용자(userId)가 pinnedParticipants에 포함되어 있는지 여부로 판단합니다.
         * - 1:1 채팅인 경우, 다른 참여자의 이름을 가져오려면 추가 조회가 필요하므로 여기서는 단순하게 title을 사용합니다.
         */
        fun from(chatRoom: ChatRoom, userId: Long): ChatRoomResponse {
            val isPinned = chatRoom.pinnedParticipants.contains(userId)
            val roomTitle = if (chatRoom.type == ChatRoomType.INDIVIDUAL) {
                // 1:1 채팅인 경우: 다른 참여자의 이름을 사용하고 싶다면 추가 조회가 필요하지만,
                // 여기서는 채팅방의 title이 있으면 사용하고, 없으면 기본 "채팅방"으로 처리합니다.
                chatRoom.title?.value ?: "채팅방"
            } else {
                // 그룹 채팅의 경우 제목 그대로 사용
                chatRoom.title?.value ?: "채팅방"
            }
            return ChatRoomResponse(
                roomId = chatRoom.id ?: 0L,
                title = roomTitle,
                lastMessage = null, // lastMessage 정보가 도메인에 없으므로 null 처리
                unreadMessages = 0, // unreadMessages 정보가 도메인에 없으므로 기본값 0 처리
                isPinned = isPinned,
                timestamp = chatRoom.lastActiveAt.atZone(ZoneId.systemDefault()).let { formatter.format(it) }
            )
        }
    }

}
