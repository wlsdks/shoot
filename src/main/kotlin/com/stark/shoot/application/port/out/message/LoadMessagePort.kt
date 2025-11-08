package com.stark.shoot.application.port.out.message

import com.stark.shoot.domain.chat.message.ChatMessage
import com.stark.shoot.domain.chat.message.vo.MessageId
import com.stark.shoot.domain.chatroom.vo.ChatRoomId
import com.stark.shoot.domain.shared.UserId
import kotlinx.coroutines.flow.Flow

interface LoadMessagePort {
    fun findById(messageId: MessageId): ChatMessage?
    fun findByRoomId(roomId: ChatRoomId, limit: Int): List<ChatMessage>
    fun findByRoomIdAndBeforeId(roomId: ChatRoomId, beforeMessageId: MessageId, limit: Int): List<ChatMessage>
    fun findByRoomIdAndAfterId(roomId: ChatRoomId, afterMessageId: MessageId, limit: Int): List<ChatMessage> // 추가

    fun findUnreadByRoomId(roomId: ChatRoomId, userId: UserId, limit: Int = 100): List<ChatMessage>
    fun findPinnedMessagesByRoomId(roomId: ChatRoomId, limit: Int): List<ChatMessage>

    /**
     * 스레드 ID로 메시지 조회 (특정 메시지의 모든 답글 조회)
     *
     * @param threadId 스레드 루트 메시지 ID
     * @return 해당 스레드에 속한 메시지 목록
     */
    fun findByThreadId(threadId: MessageId): List<ChatMessage>

    /**
     * 특정 사용자가 보낸 모든 메시지 조회
     *
     * 사용처: User 삭제 시 MongoDB 클린업
     *
     * @param senderId 발신자 사용자 ID
     * @return 해당 사용자가 보낸 모든 메시지 목록
     */
    fun findBySenderId(senderId: UserId): List<ChatMessage>

    fun findByRoomIdFlow(roomId: ChatRoomId, limit: Int): Flow<ChatMessage>
    fun findByRoomIdAndBeforeIdFlow(roomId: ChatRoomId, beforeMessageId: MessageId, limit: Int): Flow<ChatMessage>
    fun findByRoomIdAndAfterIdFlow(roomId: ChatRoomId, afterMessageId: MessageId, limit: Int): Flow<ChatMessage>

    /**
     * 여러 메시지 ID로 메시지를 배치 조회
     * N+1 쿼리 문제를 방지하기 위한 배치 조회
     *
     * 사용처: 채팅방 목록 조회 시 마지막 메시지 정보를 배치로 가져오기
     *
     * @param messageIds 조회할 메시지 ID 목록
     * @return 메시지 목록 (존재하지 않는 ID는 제외)
     */
    fun findAllByIds(messageIds: List<MessageId>): List<ChatMessage>
}
