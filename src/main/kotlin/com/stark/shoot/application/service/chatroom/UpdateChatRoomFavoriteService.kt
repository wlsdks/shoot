package com.stark.shoot.application.service.chatroom

import com.stark.shoot.application.port.`in`.chatroom.UpdateChatRoomFavoriteUseCase
import com.stark.shoot.application.port.out.LoadChatRoomPort
import com.stark.shoot.application.port.out.SaveChatRoomPort
import com.stark.shoot.domain.chat.room.ChatRoom
import com.stark.shoot.infrastructure.common.exception.ResourceNotFoundException
import com.stark.shoot.infrastructure.common.util.toObjectId
import org.springframework.stereotype.Service

@Service
class UpdateChatRoomFavoriteService(
    private val loadChatRoomPort: LoadChatRoomPort,
    private val saveChatRoomPort: SaveChatRoomPort
) : UpdateChatRoomFavoriteUseCase {

    /**
     * 사용자(userId)가 해당 채팅방(roomId)을 즐겨찾기(고정)로 설정하거나 해제합니다.
     * @param roomId 대상 채팅방 ID (String)
     * @param userId 사용자 ID (String)
     * @param isFavorite true면 즐겨찾기, false면 해제
     * @return 업데이트된 ChatRoom 도메인 객체
     * @throws ResourceNotFoundException 채팅방 또는 참여자 정보를 찾지 못한 경우
     */
    override fun updateFavoriteStatus(
        roomId: String,
        userId: String,
        isFavorite: Boolean
    ): ChatRoom {
        // 채팅방 조회 (도메인 객체로 반환)
        val chatRoom: ChatRoom = loadChatRoomPort.findById(roomId.toObjectId())
            ?: throw ResourceNotFoundException("채팅방을 찾을 수 없습니다. id=$roomId")

        // 참여자 메타데이터 업데이트: 사용자의 즐겨찾기 플래그 변경
        val updatedParticipants = chatRoom.metadata.participantsMetadata.mapValues { (participantId, participant) ->
            if (participantId == userId.toObjectId()) {
                participant.copy(isPinned = isFavorite)
            } else {
                participant
            }
        }

        // 채팅방 업데이트
        val updatedChatRoom = chatRoom.copy(
            metadata = chatRoom.metadata.copy(
                participantsMetadata = updatedParticipants
            )
        )

        // 저장
        return saveChatRoomPort.save(updatedChatRoom)
    }

}
