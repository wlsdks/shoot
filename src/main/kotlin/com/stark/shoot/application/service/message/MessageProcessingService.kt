package com.stark.shoot.application.service.message

import com.stark.shoot.application.port.`in`.message.ProcessMessageUseCase
import com.stark.shoot.application.port.out.EventPublisher
import com.stark.shoot.application.port.out.LoadChatRoomPort
import com.stark.shoot.application.port.out.SaveChatMessagePort
import com.stark.shoot.application.port.out.SaveChatRoomPort
import com.stark.shoot.domain.chat.event.ChatMessageSentEvent
import com.stark.shoot.domain.chat.event.ChatUnreadCountUpdatedEvent
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
     * 메시지 저장 후 채팅방의 메타데이터(특히 unreadCount)를 업데이트합니다.
     * 이후 메시지 전송 이벤트와 unreadCount 업데이트 이벤트를 발행하여 WebSocket을 통해 실시간 업데이트를 전파합니다.
     */
    override fun processMessage(message: ChatMessage): ChatMessage {
        val roomObjectId = message.roomId.toObjectId()
        val chatRoom = loadChatRoomPort.findById(roomObjectId)
            ?: throw ResourceNotFoundException("채팅방을 찾을 수 없습니다. roomId=${message.roomId}")

        // 메시지 저장
        val savedMessage = saveChatMessagePort.save(message)

        // sender를 제외한 각 참여자의 unreadCount 증가
        val senderObjectId = ObjectId(message.senderId)
        val updatedParticipants = chatRoom.metadata.participantsMetadata.mapValues { (participantId, participant) ->
            if (participantId != senderObjectId) {
                participant.copy(unreadCount = participant.unreadCount + 1)
            } else {
                participant
            }
        }

        // 채팅방 업데이트 (마지막 메시지 및 unreadCount 반영)
        val updatedRoom = chatRoom.copy(
            metadata = chatRoom.metadata.copy(
                participantsMetadata = updatedParticipants
            ),
            lastMessageId = savedMessage.id
        )
        saveChatRoomPort.save(updatedRoom)

        // 메시지 발송 이벤트 (예: 로그 출력 또는 추가 처리를 위해)
        eventPublisher.publish(ChatMessageSentEvent(savedMessage))

        // unreadCount 업데이트 이벤트 발행
        val unreadCounts: Map<String, Int> = updatedParticipants.mapKeys { it.key.toString() }
            .mapValues { it.value.unreadCount }

        eventPublisher.publish(
            ChatUnreadCountUpdatedEvent(
                roomId = roomObjectId.toString(),
                unreadCounts = unreadCounts
            )
        )

        return savedMessage
    }

}
