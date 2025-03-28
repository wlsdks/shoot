package com.stark.shoot.application.filter.message.impl

import com.stark.shoot.application.filter.common.MessageProperty
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
        val chatRoom = loadChatRoomPort.findById(message.roomId.toObjectId())
            ?: throw ResourceNotFoundException("채팅방을 찾을 수 없습니다. roomId=${message.roomId}")
        
        val senderObjectId = ObjectId(message.senderId)
        
        // 각 참여자의 unreadCount 업데이트
        val updatedParticipants = chatRoom.metadata.participantsMetadata.mapValues { (participantId, participant) ->
            // Redis에서 참여자가 방에 있는지(active) 확인
            val isActive = redisTemplate.opsForValue()
                .get("active:$participantId:${message.roomId}")?.toBoolean() ?: false
            
            // 발신자가 아니고 방에 없으면 unreadCount 증가
            if (participantId != senderObjectId && !isActive) {
                participant.copy(unreadCount = participant.unreadCount + 1)
            } else {
                participant
            }
        }
        
        // MessageProperty 클래스를 추가하여 메시지 처리에 필요한 부가 정보 전달
        val property = MessageProperty(
            updatedParticipants = updatedParticipants
        )
        
        val processedMessage = chain.proceed(message)
        processedMessage.metadata["property"] = property
        
        return processedMessage
    }

}