package com.stark.shoot.adapter.`in`.web.dto.chatroom

import com.stark.shoot.domain.chat.room.ChatRoom
import org.bson.types.ObjectId
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class ChatRoomResponse(
    val roomId: String,
    val title: String,
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
         */
        fun from(chatRoom: ChatRoom, userId: ObjectId): ChatRoomResponse {
            val participant = chatRoom.metadata.participantsMetadata[userId]
            return ChatRoomResponse(
                roomId = chatRoom.id ?: "",
                title = chatRoom.metadata.title ?: "채팅방",
                // lastMessage가 null이면 기본 텍스트를 넣습니다.
                lastMessage = chatRoom.lastMessageId ?: "최근 메시지가 없습니다.",
                unreadMessages = participant?.unreadCount ?: 0,
                isPinned = participant?.isPinned ?: false,
                // Instant를 ZonedDateTime으로 변환한 후 포맷 (시스템 기본 타임존 사용)
                timestamp = chatRoom.lastActiveAt.atZone(ZoneId.systemDefault()).let { formatter.format(it) }
            )
        }
    }

}