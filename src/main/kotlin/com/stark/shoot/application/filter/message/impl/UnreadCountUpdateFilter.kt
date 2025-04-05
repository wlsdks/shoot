package com.stark.shoot.application.filter.message.impl

import com.stark.shoot.application.filter.message.chain.MessageProcessingChain
import com.stark.shoot.application.filter.common.MessageProcessingFilter
import com.stark.shoot.application.port.out.chatroom.LoadChatRoomPort
import com.stark.shoot.domain.chat.message.ChatMessage
import com.stark.shoot.infrastructure.exception.web.ResourceNotFoundException
import com.stark.shoot.infrastructure.util.toObjectId
import org.bson.types.ObjectId
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component

@Component
class UnreadCountUpdateFilter(
    private val loadChatRoomPort: LoadChatRoomPort,
    private val redisTemplate: StringRedisTemplate
) : MessageProcessingFilter {
    
    override suspend fun process(
        message: ChatMessage,
        chain: MessageProcessingChain
    ): ChatMessage {
        val chatRoom = loadChatRoomPort.findById(message.roomId)
            ?: throw ResourceNotFoundException("채팅방을 찾을 수 없습니다. roomId=${message.roomId}")

    }

}