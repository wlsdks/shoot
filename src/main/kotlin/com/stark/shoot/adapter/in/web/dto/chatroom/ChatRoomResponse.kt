package com.stark.shoot.adapter.`in`.web.dto.chatroom

import com.stark.shoot.adapter.out.persistence.mongodb.document.room.embedded.type.ChatRoomType
import com.stark.shoot.domain.chat.room.ChatRoom
import org.bson.types.ObjectId
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class ChatRoomResponse(
    val roomId: String,
    val title: String,  // 여기서는 '채팅방 제목' 대신 상대방 이름을 담습니다.
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
         * 만약 채팅방 타입이 INDIVIDUAL이면, title 대신 상대방의 닉네임(또는 이름)을 반환합니다.
         */
        fun from(chatRoom: ChatRoom, userId: ObjectId): ChatRoomResponse {
            // 현재 사용자의 메타데이터
            val participant = chatRoom.metadata.participantsMetadata[userId]
            // 1:1 채팅인 경우, 현재 userId를 제외한 다른 참여자의 닉네임을 가져옵니다.
            val roomTitle = if (chatRoom.metadata.type == ChatRoomType.INDIVIDUAL) {
                // 채팅방 참여자들 중 현재 사용자가 아닌 다른 사용자를 찾습니다.
                val otherParticipantId = chatRoom.participants.firstOrNull { it != userId }
                // 해당 사용자의 메타데이터에서 nickname을 가져옵니다. 없으면 fallback으로 chatRoom.metadata.title 사용.
                otherParticipantId?.let { chatRoom.metadata.participantsMetadata[it]?.nickname }
                    ?: (chatRoom.metadata.title ?: "채팅방")
            } else {
                // 그룹 채팅 등 다른 타입이면, 기존 제목을 사용합니다.
                chatRoom.metadata.title ?: "채팅방"
            }
            return ChatRoomResponse(
                roomId = chatRoom.id ?: "",
                title = roomTitle,
                // 마지막 메시지 텍스트를 사용합니다.
                // 만약 도메인에 lastMessageText 필드가 있다면 이를 사용하고, 그렇지 않으면 lastMessageId를 그대로 표시하는 로직으로 수정 필요
                lastMessage = chatRoom.lastMessageText ?: "최근 메시지가 없습니다.",
                unreadMessages = participant?.unreadCount ?: 0,
                isPinned = participant?.isPinned ?: false,
                timestamp = chatRoom.lastActiveAt.atZone(ZoneId.systemDefault()).let { formatter.format(it) }
            )
        }
    }
}