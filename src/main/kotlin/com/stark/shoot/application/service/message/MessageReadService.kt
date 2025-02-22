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

        // 참여자 정보 업데이트 (사용자의 안 읽은 메시지 수를 0으로 만들고, 마지막 읽은 시간을 지금으로 설정.)
        val updatedParticipant = participantMeta.copy(
            unreadCount = 0,
            lastReadAt = Instant.now()
        )

        // 채팅방 업데이트 (갱신된 사용자 정보로 교체.)
        val updatedParticipants = chatRoom.metadata.participantsMetadata.toMutableMap()
        updatedParticipants[participantId] = updatedParticipant

        // 채팅방 전체를 새 정보로 업데이트 (최근 메시지의 readBy 업데이트를 위해) 및 저장
        val updatedRoom = chatRoom.copy(metadata = chatRoom.metadata.copy(participantsMetadata = updatedParticipants))
        saveChatRoomPort.save(updatedRoom)

        // 모든 메시지의 readBy 업데이트 (최근 메시지 대상으로 예시, 마지막 메시지 ID가 있으면 실행.)
        chatRoom.lastMessageId?.let { messageId ->
            // 마지막 메시지를 DB에서 가져와 → 없으면 스킵.
            val message = loadChatMessagePort.findById(messageId.toObjectId()) ?: return@let

            // 메시지의 readBy에 누가 읽었는지 추가 → 저장 → 웹소켓으로 전송.
            val updatedMessage = message.copy(readBy = message.readBy.toMutableMap().apply { put(userId, true) })
            saveChatMessagePort.save(updatedMessage)
            messagingTemplate.convertAndSend("/topic/messages/$roomId", updatedMessage)
        }
    }

}