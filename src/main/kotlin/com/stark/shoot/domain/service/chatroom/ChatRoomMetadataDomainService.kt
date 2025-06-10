package com.stark.shoot.domain.service.chatroom

import com.stark.shoot.domain.chat.message.ChatMessage
import com.stark.shoot.domain.chat.room.ChatRoom
import java.time.Instant

/**
 * 채팅방 메타데이터 관련 도메인 서비스
 * 채팅방의 메타데이터(마지막 메시지, 활동 시간 등) 업데이트를 담당합니다.
 */
class ChatRoomMetadataDomainService {

    /**
     * 새 메시지가 추가될 때 채팅방 메타데이터를 업데이트합니다.
     *
     * @param chatRoom 업데이트할 채팅방
     * @param message 추가된 메시지
     * @return 업데이트된 채팅방
     */
    fun updateChatRoomWithNewMessage(
        chatRoom: ChatRoom,
        message: ChatMessage
    ): ChatRoom {
        // 메시지 ID가 없는 경우 예외 발생
        val messageId = message.id ?: throw IllegalArgumentException("메시지 ID가 없습니다.")

        // 채팅방 메타데이터 업데이트 (마지막 메시지 ID, 마지막 활동 시간)
        return chatRoom.update(
            lastMessageId = messageId,
            lastActiveAt = Instant.now()
        )
    }

}