package com.stark.shoot.application.service.chat

import com.stark.shoot.application.port.`in`.chat.ProcessMessageUseCase
import com.stark.shoot.application.port.out.EventPublisher
import com.stark.shoot.application.port.out.LoadChatRoomPort
import com.stark.shoot.application.port.out.SaveChatMessagePort
import com.stark.shoot.application.port.out.SaveChatRoomPort
import com.stark.shoot.domain.chat.event.ChatMessageSentEvent
import com.stark.shoot.domain.chat.message.ChatMessage
import com.stark.shoot.infrastructure.common.exception.ResourceNotFoundException
import com.stark.shoot.infrastructure.common.util.toObjectId
import org.bson.types.ObjectId
import org.springframework.stereotype.Service

@Service
class MessageProcessingService(
    private val saveChatMessagePort: SaveChatMessagePort,
    private val loadChatRoomPort: LoadChatRoomPort,
    private val saveChatRoomPort: SaveChatRoomPort,
    private val eventPublisher: EventPublisher
) : ProcessMessageUseCase {

    /**
     * 메시지 저장 및 채팅방 메타데이터 업데이트 담당
     * @param message 메시지
     */
    override fun processMessage(message: ChatMessage): ChatMessage {
        val roomObjectId = message.roomId.toObjectId()
        val chatRoom = loadChatRoomPort.findById(roomObjectId)
            ?: throw ResourceNotFoundException("채팅방을 찾을 수 없습니다. roomId=${message.roomId}")

        // 메시지 저장
        val savedMessage = saveChatMessagePort.save(message)

        // 채팅방 메타데이터 업데이트
        val senderObjectId = ObjectId(message.senderId)
        val updatedParticipants = chatRoom.metadata.participantsMetadata.mapValues { (participantId, participantDoc) ->
            if (participantId != senderObjectId) {
                participantDoc.copy(unreadCount = participantDoc.unreadCount + 1)
            } else {
                participantDoc
            }
        }

        val updatedRoom = chatRoom.copy(
            metadata = chatRoom.metadata.copy(
                participantsMetadata = updatedParticipants
            ),
            lastMessageId = savedMessage.id
        )

        saveChatRoomPort.save(updatedRoom)
        eventPublisher.publish(ChatMessageSentEvent(savedMessage))

        return savedMessage
    }

}
