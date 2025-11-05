package com.stark.shoot.domain.chatroom.service

import com.stark.shoot.domain.chatroom.ChatRoom
import com.stark.shoot.domain.shared.event.ChatRoomCreatedEvent
import com.stark.shoot.domain.exception.ChatRoomException

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
        val roomId = chatRoom.id ?: throw ChatRoomException.MissingId()

        // 각 참여자에 대한 이벤트 생성
        return chatRoom.participants.map { participantId ->
            ChatRoomCreatedEvent.create(
                roomId = roomId,
                userId = participantId
            )
        }
    }

}