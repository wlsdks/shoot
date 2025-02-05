package com.stark.shoot.application.service.chat

import com.stark.shoot.application.port.`in`.chat.MessageReadUseCase
import com.stark.shoot.application.port.out.LoadChatRoomPort
import com.stark.shoot.application.port.out.SaveChatRoomPort
import com.stark.shoot.infrastructure.common.util.toObjectId
import org.bson.types.ObjectId
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class MessageReadServiceImpl(
    private val loadChatRoomPort: LoadChatRoomPort,
    private val saveChatRoomPort: SaveChatRoomPort
) : MessageReadUseCase {

    override fun markRead(roomId: String, userId: String) {
        // 채팅방 ID와 참여자 ID를 ObjectId로 변환
        val roomObjectId: ObjectId = roomId.toObjectId()
        val participantId: ObjectId = userId.toObjectId()

        // 채팅방 조회
        val chatRoom = loadChatRoomPort.findById(roomObjectId)
            ?: throw IllegalArgumentException("채팅방을 찾을 수 없습니다.")

        // 참여자 메타데이터 조회
        val participantMeta = chatRoom.metadata.participantsMetadata[participantId]
            ?: throw IllegalArgumentException("참여자를 찾을 수 없습니다.")

        // 참여자 메타데이터를 업데이트하기 위해 복사본 생성
        val updatedParticipant = participantMeta.copy(
            unreadCount = 0,           // 읽지 않은 메시지 수는 0으로 초기화
            lastReadMessageId = null,  // 마지막으로 읽은 메시지 ID는 없음
            lastReadAt = Instant.now() // 마지막으로 읽은 시간은 현재 시간으로 설정
        )

        // 참여자 메타데이터 업데이트
        val updatedParticipants = chatRoom.metadata.participantsMetadata.toMutableMap()
        updatedParticipants[participantId] = updatedParticipant

        // 채팅방 메타데이터 업데이트
        val updatedMetadata = chatRoom.metadata.copy(
            participantsMetadata = updatedParticipants
        )

        // 채팅방 정보 업데이트
        val updatedChatRoom = chatRoom.copy(
            metadata = updatedMetadata
        )

        // 채팅방 정보 저장
        saveChatRoomPort.save(updatedChatRoom)
    }

}