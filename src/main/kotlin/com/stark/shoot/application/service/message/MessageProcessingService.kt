package com.stark.shoot.application.service.message

import com.stark.shoot.application.port.`in`.message.ProcessMessageUseCase
import com.stark.shoot.application.port.out.EventPublisher
import com.stark.shoot.application.port.out.LoadChatRoomPort
import com.stark.shoot.application.port.out.SaveChatMessagePort
import com.stark.shoot.application.port.out.SaveChatRoomPort
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
     * 메시지 저장 및 채팅방 메타데이터 업데이트 담당
     * 새 메시지가 도착할 때 sender를 제외한 다른 참여자의 unreadCount가 증가하고,
     * 사용자가 해당 채팅방을 열거나 스크롤하여 메시지를 확인하면(예: "모두 읽음" 버튼 또는 자동 감지), unreadCount가 0으로 업데이트되도록 하면 됩니다.
     */
    override fun processMessage(message: ChatMessage): ChatMessage {
        val roomObjectId = message.roomId.toObjectId()
        val chatRoom = loadChatRoomPort.findById(roomObjectId)
            ?: throw ResourceNotFoundException("채팅방을 찾을 수 없습니다. roomId=${message.roomId}")

        // 메시지 저장
        val savedMessage = saveChatMessagePort.save(message)

        // 보낸 사람(sender)을 제외한 참여자의 unreadCount를 1 증가시킵니다.
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
