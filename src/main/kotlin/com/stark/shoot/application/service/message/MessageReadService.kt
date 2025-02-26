package com.stark.shoot.application.service.message

import com.stark.shoot.application.port.`in`.message.MessageReadUseCase
import com.stark.shoot.application.port.out.LoadChatMessagePort
import com.stark.shoot.application.port.out.LoadChatRoomPort
import com.stark.shoot.application.port.out.SaveChatMessagePort
import com.stark.shoot.application.port.out.SaveChatRoomPort
import com.stark.shoot.domain.chat.room.ChatRoom
import com.stark.shoot.domain.chat.room.Participant
import com.stark.shoot.infrastructure.common.util.toObjectId
import org.bson.types.ObjectId
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class MessageReadService(
    private val loadChatRoomPort: LoadChatRoomPort,
    private val saveChatRoomPort: SaveChatRoomPort,
    private val saveChatMessagePort: SaveChatMessagePort,
    private val loadChatMessagePort: LoadChatMessagePort
) : MessageReadUseCase {

    override fun markRead(roomId: String, userId: String) {
        // 채팅방 및 참여자 정보 조회
        val roomObjectId = roomId.toObjectId()
        val chatRoom = loadChatRoomPort.findById(roomObjectId) ?: throw IllegalArgumentException("채팅방 없습니다.")
        val participantId = ObjectId(userId)
        val participantMeta = chatRoom.metadata.participantsMetadata[participantId]
            ?: throw IllegalArgumentException("참여자 없습니다.")

        // 채팅방 내부의 안 읽은 메시지 수를 0으로 업데이트하고, 마지막 읽은 시간을 현재 시간으로 설정
        updateParticipantReadStatus(participantMeta, chatRoom, participantId)

        // 사용자가 안읽었던 모든 메시지에 대해서 읽음 여부를 true로 업데이트
        markAllUnreadMessagesAsRead(roomId, userId)
    }

    /**
     * 채팅방의 안 읽은 메시지 수를 0으로 업데이트하고, 마지막 읽은 시간을 현재 시간으로 설정합니다.
     */
    private fun updateParticipantReadStatus(
        participantMeta: Participant,
        chatRoom: ChatRoom,
        participantId: ObjectId
    ) {
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
    }

    /**
     * 사용자가 읽은 메시지를 업데이트합니다.
     */
    private fun markAllUnreadMessagesAsRead(
        roomId: String,
        userId: String
    ) {
        // 사용자의 모든 안 읽은 메시지 조회
        val unReadMessages = loadChatMessagePort
            .findUnreadByRoomId(roomId.toObjectId(), userId.toObjectId()) ?: emptyList()

        // 안 읽은 메시지에 사용자가 읽었음을 표시 (readBy 필드 업데이트)
        unReadMessages.forEach { message ->
            // 사용자가 읽었음을 표시
            val updatedMessage = message.copy(
                readBy = message.readBy.toMutableMap().apply { put(userId, true) }
            )

            // 메시지 저장
            saveChatMessagePort.save(updatedMessage)
        }
    }

}