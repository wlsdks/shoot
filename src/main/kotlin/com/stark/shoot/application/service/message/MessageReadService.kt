package com.stark.shoot.application.service.message

import com.stark.shoot.application.port.`in`.message.MessageReadUseCase
import com.stark.shoot.application.port.out.LoadChatMessagePort
import com.stark.shoot.application.port.out.LoadChatRoomPort
import com.stark.shoot.application.port.out.SaveChatMessagePort
import com.stark.shoot.application.port.out.SaveChatRoomPort
import com.stark.shoot.infrastructure.common.util.toObjectId
import org.bson.types.ObjectId
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class MessageReadService(
    private val loadChatRoomPort: LoadChatRoomPort,
    private val saveChatRoomPort: SaveChatRoomPort,
    private val saveChatMessagePort: SaveChatMessagePort,
    private val loadChatMessagePort: LoadChatMessagePort,
    private val messagingTemplate: SimpMessagingTemplate
) : MessageReadUseCase {

    override fun markRead(roomId: String, userId: String) {
        // 채팅방 및 참여자 정보 조회
        val roomObjectId = roomId.toObjectId()
        val chatRoom = loadChatRoomPort.findById(roomObjectId) ?: throw IllegalArgumentException("채팅방 없습니다.")
        val participantId = ObjectId(userId)
        val participantMeta = chatRoom.metadata.participantsMetadata[participantId]
            ?: throw IllegalArgumentException("참여자 없습니다.")

        // 참여자 정보 업데이트 (unreadCount 0, lastReadAt 현재 시간)
        val updatedParticipant = participantMeta.copy(
            unreadCount = 0,
            lastReadAt = Instant.now()
        )

        // 채팅방 업데이트
        val updatedParticipants = chatRoom.metadata.participantsMetadata.toMutableMap()
        updatedParticipants[participantId] = updatedParticipant
        val updatedRoom = chatRoom.copy(metadata = chatRoom.metadata.copy(participantsMetadata = updatedParticipants))
        saveChatRoomPort.save(updatedRoom)

        // 모든 메시지의 readBy 업데이트 (최근 메시지 대상으로 예시)
        chatRoom.lastMessageId?.let { messageId ->
            val message = loadChatMessagePort.findById(messageId.toObjectId()) ?: return@let
            val updatedMessage = message.copy(readBy = message.readBy.toMutableMap().apply { put(userId, true) })
            saveChatMessagePort.save(updatedMessage)
            messagingTemplate.convertAndSend("/topic/messages/$roomId", updatedMessage)
        }
    }

}