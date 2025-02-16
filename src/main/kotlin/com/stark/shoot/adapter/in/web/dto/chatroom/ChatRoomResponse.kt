package com.stark.shoot.adapter.`in`.web.dto.chatroom

import com.stark.shoot.domain.chat.room.ChatRoom
import org.bson.types.ObjectId

data class ChatRoomResponse(
    val roomId: String,
    val title: String,
    val lastMessage: String?,
    val unreadMessages: Int,
    val isPinned: Boolean
) {

    companion object {
        /**
         * 특정 사용자의 관점에서 도메인 ChatRoom을 DTO로 변환합니다.
         */
        fun from(chatRoom: ChatRoom, userId: ObjectId): ChatRoomResponse {
            val participant = chatRoom.metadata.participantsMetadata[userId]

            return ChatRoomResponse(
                roomId = chatRoom.id ?: "",
                title = chatRoom.metadata.title ?: "채팅방",
                lastMessage = chatRoom.lastMessageId, // 만약 별도의 로직이 있다면 수정
                unreadMessages = participant?.unreadCount ?: 0,
                isPinned = participant?.isPinned ?: false
            )
        }
    }

}