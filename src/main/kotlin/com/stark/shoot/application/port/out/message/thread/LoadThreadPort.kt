package com.stark.shoot.application.port.out.message.thread

import com.stark.shoot.domain.chat.message.ChatMessage
import com.stark.shoot.domain.chat.message.vo.MessageId
import com.stark.shoot.domain.chatroom.vo.ChatRoomId

interface LoadThreadPort {
    fun findByThreadId(threadId: MessageId, limit: Int): List<ChatMessage>
    fun findByThreadIdAndBeforeId(threadId: MessageId, beforeMessageId: MessageId, limit: Int): List<ChatMessage>
    fun findThreadRootsByRoomId(roomId: ChatRoomId, limit: Int): List<ChatMessage>
    fun findThreadRootsByRoomIdAndBeforeId(
        roomId: ChatRoomId,
        beforeMessageId: MessageId,
        limit: Int
    ): List<ChatMessage>

    fun countByThreadId(threadId: MessageId): Long
    
    /**
     * 여러 스레드 ID에 대한 답글 수를 배치로 조회합니다.
     * N+1 쿼리 문제를 해결하기 위해 사용됩니다.
     * 
     * @param threadIds 답글 수를 조회할 스레드 ID 목록
     * @return 스레드 ID를 키로 하고 답글 수를 값으로 하는 Map
     */
    fun countByThreadIds(threadIds: List<MessageId>): Map<MessageId, Long>
}