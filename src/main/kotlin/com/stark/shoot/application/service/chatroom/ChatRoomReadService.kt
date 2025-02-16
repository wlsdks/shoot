package com.stark.shoot.application.service.chatroom

import com.stark.shoot.application.port.`in`.chatroom.ChatRoomReadUseCase
import com.stark.shoot.application.port.out.EventPublisher
import com.stark.shoot.application.port.out.LoadChatRoomPort
import com.stark.shoot.application.port.out.SaveChatRoomPort
import com.stark.shoot.domain.chat.event.ChatUnreadCountUpdatedEvent
import com.stark.shoot.infrastructure.common.exception.ResourceNotFoundException
import com.stark.shoot.infrastructure.common.util.toObjectId
import org.springframework.stereotype.Service

@Service
class ChatRoomReadService(
    private val loadChatRoomPort: LoadChatRoomPort,
    private val saveChatRoomPort: SaveChatRoomPort,
    private val eventPublisher: EventPublisher
) : ChatRoomReadUseCase {

    /**
     * 특정 채팅방의 모든 메시지를 읽음 상태로 업데이트합니다.
     * @param roomId 채팅방 ID
     * @param userId 사용자 ID
     */
    override fun markAllAsRead(
        roomId: String,
        userId: String
    ) {
        val roomObjectId = roomId.toObjectId()
        val chatRoom = loadChatRoomPort.findById(roomObjectId)
            ?: throw ResourceNotFoundException("채팅방을 찾을 수 없습니다. roomId=$roomId")

        // 해당 사용자의 unreadCount를 0으로 업데이트
        val updatedParticipants = chatRoom.metadata.participantsMetadata.mapValues { (participantId, participant) ->
            if (participantId == userId.toObjectId()) {
                participant.copy(unreadCount = 0)
            } else {
                participant
            }
        }

        val updatedChatRoom = chatRoom.copy(
            metadata = chatRoom.metadata.copy(
                participantsMetadata = updatedParticipants
            )
        )

        saveChatRoomPort.save(updatedChatRoom)

        // 해당 사용자의 unreadCount 업데이트 이벤트 발행 (값은 0)
        eventPublisher.publish(
            ChatUnreadCountUpdatedEvent(
                roomId = roomId,
                unreadCounts = mapOf(userId to 0)
            )
        )
    }

}
