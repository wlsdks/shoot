package com.stark.shoot.application.port.out.message

import com.stark.shoot.domain.chat.message.ChatMessage
import com.stark.shoot.domain.chat.message.vo.MessageId
import com.stark.shoot.domain.chatroom.vo.ChatRoomId

interface LoadThreadPort {

    fun findByThreadId(threadId: MessageId, limit: Int): List<ChatMessage>
    fun findByThreadIdAndBeforeId(threadId: MessageId, beforeMessageId: MessageId, limit: Int): List<ChatMessage>

    fun findThreadRootsByRoomId(roomId: ChatRoomId, limit: Int): List<ChatMessage>
    fun findThreadRootsByRoomIdAndBeforeId(roomId: ChatRoomId, beforeMessageId: MessageId, limit: Int): List<ChatMessage>
    fun countByThreadId(threadId: MessageId): Long

}
