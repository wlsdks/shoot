package com.stark.shoot.application.service.chatroom

import com.stark.shoot.application.port.`in`.chatroom.UpdateChatRoomFavoriteUseCase
import com.stark.shoot.application.port.out.LoadChatRoomPort
import com.stark.shoot.application.port.out.SaveChatRoomPort
import com.stark.shoot.application.port.out.chatroom.LoadUserPinnedRoomsPort
import com.stark.shoot.domain.chat.room.ChatRoom
import com.stark.shoot.infrastructure.common.exception.ResourceNotFoundException
import com.stark.shoot.infrastructure.common.util.toObjectId
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class UpdateChatRoomFavoriteService(
    private val loadChatRoomPort: LoadChatRoomPort,
    private val saveChatRoomPort: SaveChatRoomPort,
    private val loadUserPinnedRoomsPort: LoadUserPinnedRoomsPort
) : UpdateChatRoomFavoriteUseCase {

    companion object {
        private const val MAX_PINNED = 5
    }

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

        // 현재 사용자의 핀 채팅방 목록 조회
        val pinnedRooms = loadUserPinnedRoomsPort.findByUserId(userId)

        // 참여자 메타데이터 업데이트: 사용자의 즐겨찾기 플래그 변경
        val updatedParticipants = chatRoom.metadata.participantsMetadata.mapValues { (participantId, participant) ->
            // 참여자 ID가 사용자 ID와 일치하면 즐겨찾기 상태 변경
            if (participantId == userId.toObjectId()) {
                if (isFavorite) {
                    // 새 핀 요청: 이미 핀 상태가 아니면서 최대 제한에 도달한 경우 예외 발생
                    if (pinnedRooms.size >= MAX_PINNED && !participant.isPinned) {
                        throw IllegalStateException("최대 핀 채팅방 개수를 초과했습니다. (MAX_PINNED=$MAX_PINNED)")
                    }
                    // 핀 상태 true와 함께 현재 시간 저장 (추가 필드: pinTimestamp 필요)
                    participant.copy(isPinned = true, pinTimestamp = Instant.now())
                } else {
                    // 핀 해제 요청
                    participant.copy(isPinned = false, pinTimestamp = null)
                }
            } else {
                // 다른 참여자는 그대로 유지
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
