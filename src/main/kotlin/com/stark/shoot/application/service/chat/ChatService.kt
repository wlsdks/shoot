package com.stark.shoot.application.service.chat

import com.stark.shoot.application.port.`in`.SendMessageUseCase
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
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class ChatService(
    private val saveChatMessagePort: SaveChatMessagePort,
    private val loadChatRoomPort: LoadChatRoomPort,
    private val saveChatRoomPort: SaveChatRoomPort,
    private val eventPublisher: EventPublisher
) : SendMessageUseCase {

    @Transactional
    override fun sendMessage(
        roomId: String,
        senderId: String,
        messageContent: ChatMessage
    ): ChatMessage {
        val roomObjectId = roomId.toObjectId()
        val chatRoom = loadChatRoomPort.findById(roomObjectId)
            ?: throw ResourceNotFoundException("채팅방을 찾을 수 없습니다. roomId=$roomId")

        // 메시지 생성 및 저장
        val chatMessage = ChatMessage(
            roomId = roomId,
            senderId = senderId,
            content = messageContent.content,
            status = messageContent.status
        )
        val savedMessage = saveChatMessagePort.save(chatMessage)

        // sender를 ObjectId로 변환
        val senderObjectId = ObjectId(senderId)

        // sender 제외한 다른 참여자들의 unreadCount +1
        val updatedParticipants = chatRoom.metadata.participantsMetadata.mapValues { (participantId, participantDoc) ->
            if (participantId != senderObjectId) {
                participantDoc.copy(unreadCount = participantDoc.unreadCount + 1)
            } else {
                participantDoc
            }
        }

        // 메타데이터 copy
        val updatedMetadata = chatRoom.metadata.copy(
            participantsMetadata = updatedParticipants
        )

        // 채팅방 copy
        val updatedRoom = chatRoom.copy(
            metadata = updatedMetadata,
            lastMessageId = savedMessage.id
        )

        // 채팅방 저장
        saveChatRoomPort.save(updatedRoom)

        // 이벤트 발행
        eventPublisher.publish(ChatMessageSentEvent(savedMessage))

        return savedMessage
    }


    @Transactional
    fun markMessagesAsRead(roomId: String, userId: String) {
        val roomObjectId = ObjectId(roomId)
        val chatRoom = loadChatRoomPort.findById(roomObjectId)
            ?: throw ResourceNotFoundException("채팅방을 찾을 수 없습니다.")

        val userObjectId = ObjectId(userId)

        val updatedParticipants = chatRoom.metadata.participantsMetadata.mapValues { (participantId, participantDoc) ->
            if (participantId == userObjectId) {
                participantDoc.copy(
                    unreadCount = 0,
                    lastReadMessageId = chatRoom.lastMessageId, // todo: ObjectId 이어야 하는지 확인
                    lastReadAt = Instant.now()
                )
            } else {
                participantDoc
            }
        }

        val updatedMetadata = chatRoom.metadata.copy(
            participantsMetadata = updatedParticipants
        )

        val updatedRoom = chatRoom.copy(
            metadata = updatedMetadata
        )
        saveChatRoomPort.save(updatedRoom)
    }

}