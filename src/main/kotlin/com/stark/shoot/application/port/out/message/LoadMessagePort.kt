package com.stark.shoot.application.port.out.message

import com.stark.shoot.domain.chat.message.ChatMessage
import com.stark.shoot.domain.chatroom.vo.ChatRoomId
import com.stark.shoot.domain.chat.message.vo.MessageId
import com.stark.shoot.domain.user.vo.UserId
import kotlinx.coroutines.flow.Flow

interface LoadMessagePort {
    fun findById(messageId: MessageId): ChatMessage?
    fun findByRoomId(roomId: ChatRoomId, limit: Int): List<ChatMessage>
    fun findByRoomIdAndBeforeId(roomId: ChatRoomId, beforeMessageId: MessageId, limit: Int): List<ChatMessage>
    fun findByRoomIdAndAfterId(roomId: ChatRoomId, afterMessageId: MessageId, limit: Int): List<ChatMessage> // 추가

    fun findUnreadByRoomId(roomId: ChatRoomId, userId: UserId, limit: Int = 100): List<ChatMessage>
    fun findPinnedMessagesByRoomId(roomId: ChatRoomId, limit: Int): List<ChatMessage>

    fun findByRoomIdFlow(roomId: ChatRoomId, limit: Int): Flow<ChatMessage>
    fun findByRoomIdAndBeforeIdFlow(roomId: ChatRoomId, beforeMessageId: MessageId, limit: Int): Flow<ChatMessage>
    fun findByRoomIdAndAfterIdFlow(roomId: ChatRoomId, afterMessageId: MessageId, limit: Int): Flow<ChatMessage>
}
