package com.stark.shoot.domain.service.chatroom

import com.stark.shoot.domain.chat.event.ChatRoomCreatedEvent
import com.stark.shoot.domain.chat.room.ChatRoom

/**
 * 채팅방 이벤트 관련 도메인 서비스
 * 채팅방 생성, 수정 등의 이벤트를 생성합니다.
 */
class ChatRoomEventService {

    /**
     * 채팅방 생성 이벤트를 생성합니다.
     *
     * @param chatRoom 생성된 채팅방
     * @return 생성된 도메인 이벤트 목록
     */
    fun createChatRoomCreatedEvents(chatRoom: ChatRoom): List<ChatRoomCreatedEvent> {
        val roomId = chatRoom.id?.value ?: throw IllegalArgumentException("채팅방 ID가 없습니다.")

        // 각 참여자에 대한 이벤트 생성
        return chatRoom.participants.map { participantId ->
            ChatRoomCreatedEvent.create(
                roomId = roomId,
                userId = participantId
            )
        }
    }

}