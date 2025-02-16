package com.stark.shoot.adapter.`in`.web.dto.chatroom

import com.stark.shoot.domain.chat.room.ChatRoom

data class ChatRoomResponse(
    val roomId: String,
    val title: String,
    val lastMessage: String?,
    val unreadMessages: Int   // 읽지 않은 메시지 수
) {

    companion object {
        fun from(chatRoom: ChatRoom): ChatRoomResponse {
            return ChatRoomResponse(
                roomId = chatRoom.id ?: "",
                // ChatRoom의 metadata에 제목(title)이 있다고 가정합니다.
                title = chatRoom.metadata.title ?: "채팅방",
                // 현재 예시에서는 lastMessageText가 없으므로 null로 처리합니다.
                lastMessage = null,
                // unreadMessages는 별도 계산 로직이 없다면 0으로 처리합니다.
                unreadMessages = 0
            )
        }
    }

}