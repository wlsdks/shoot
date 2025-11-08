package com.stark.shoot.domain.chatroom.service

import com.stark.shoot.domain.chatroom.ChatRoom
import com.stark.shoot.domain.chatroom.vo.MessageId
import java.time.Instant

/**
 * 채팅방 메타데이터 관련 도메인 서비스
 * 채팅방의 메타데이터(마지막 메시지, 활동 시간 등) 업데이트를 담당합니다.
 *
 * DDD 개선: Chat Context의 ChatMessage 도메인 객체 참조 제거
 * ACL 개선: String 대신 MessageId VO 사용
 */
class ChatRoomMetadataDomainService {

    /**
     * 새 메시지가 추가될 때 채팅방 내부의 메타데이터를 업데이트합니다.
     *
     * DDD 개선: ChatMessage 객체 대신 messageId(MessageId)와 createdAt(Instant)만 받음
     *
     * @param chatRoom 업데이트할 채팅방
     * @param messageId 추가된 메시지 ID (MessageId VO)
     * @param createdAt 메시지 생성 시간
     * @return 업데이트된 채팅방
     */
    fun updateChatRoomWithNewMessage(
        chatRoom: ChatRoom,
        messageId: MessageId,
        createdAt: Instant = Instant.now()
    ): ChatRoom {
        // 채팅방 메타데이터 업데이트 (마지막 메시지 ID, 마지막 활동 시간)
        chatRoom.update(
            lastMessageId = messageId,
            lastActiveAt = createdAt
        )
        return chatRoom
    }

    /**
     * Saga 보상 시 채팅방 메타데이터를 이전 상태로 복원합니다.
     *
     * DB에서 최신 상태(version=N+1)를 조회한 후, 이전 상태의 메타데이터 값으로 복원합니다.
     * 이렇게 하면 OptimisticLockException을 피할 수 있습니다.
     *
     * @param currentRoom DB에서 조회한 최신 상태의 채팅방 (version=N+1)
     * @param previousState 복원할 이전 상태의 메타데이터 (version=N)
     * @return 이전 메타데이터로 복원된 채팅방 (version은 최신 유지)
     */
    fun restoreChatRoomMetadata(
        currentRoom: ChatRoom,
        previousState: ChatRoom
    ): ChatRoom {
        // 최신 version을 유지한 채 메타데이터만 이전 상태로 복원
        currentRoom.update(
            lastMessageId = previousState.lastMessageId,
            lastActiveAt = previousState.lastActiveAt
        )
        return currentRoom
    }

}