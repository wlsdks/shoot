package com.stark.shoot.application.service.chatroom

import com.stark.shoot.adapter.`in`.web.dto.ApiException
import com.stark.shoot.adapter.`in`.web.dto.ErrorCode
import com.stark.shoot.application.port.`in`.chatroom.UpdateChatRoomFavoriteUseCase
import com.stark.shoot.application.port.out.chatroom.LoadChatRoomPort
import com.stark.shoot.application.port.out.chatroom.LoadPinnedRoomsPort
import com.stark.shoot.application.port.out.chatroom.SaveChatRoomPort
import com.stark.shoot.domain.chat.room.ChatRoom
import com.stark.shoot.infrastructure.annotation.UseCase
import com.stark.shoot.infrastructure.exception.web.ResourceNotFoundException
import org.springframework.transaction.annotation.Transactional

@Transactional
@UseCase
class UpdateChatRoomFavoriteService(
    private val loadChatRoomPort: LoadChatRoomPort,
    private val saveChatRoomPort: SaveChatRoomPort,
    private val loadPinnedRoomsPort: LoadPinnedRoomsPort
) : UpdateChatRoomFavoriteUseCase {

    companion object {
        private const val MAX_PINNED = 5
    }

    /**
     * 사용자(userId)가 해당 채팅방(roomId)을 즐겨찾기(핀)로 설정하거나 해제합니다.
     */
    override fun updateFavoriteStatus(
        roomId: Long,
        userId: Long,
        isFavorite: Boolean
    ): ChatRoom {
        // 채팅방 조회 (도메인 객체로 반환)
        val chatRoom: ChatRoom = loadChatRoomPort.findById(roomId)
            ?: throw ResourceNotFoundException("채팅방을 찾을 수 없습니다. id=$roomId")

        // 현재 사용자가 핀 처리한 채팅방 목록 조회 (즐겨찾기 제한 체크용)
        val pinnedRooms = loadPinnedRoomsPort.findByUserId(userId)

        // pinnedParticipants 필드를 업데이트합니다.
        val updatedPinned = updatePinnedParticipants(chatRoom, userId, isFavorite, pinnedRooms)
        val updatedChatRoom = chatRoom.copy(
            pinnedParticipants = updatedPinned
        )

        return saveChatRoomPort.save(updatedChatRoom)
    }

    /**
     * pinnedParticipants 업데이트
     *
     * @param chatRoom 채팅방 도메인 객체
     * @param userId 현재 사용자 ID
     * @param isFavorite 즐겨찾기 여부
     * @param pinnedRooms 현재 사용자가 이미 핀 처리한 채팅방 목록
     * @return 업데이트된 고정 참여자 목록
     */
    private fun updatePinnedParticipants(
        chatRoom: ChatRoom,
        userId: Long,
        isFavorite: Boolean,
        pinnedRooms: List<ChatRoom>
    ): MutableSet<Long> {
        val currentPinned = chatRoom.pinnedParticipants.toMutableSet()

        // 이미 즐겨찾기된 채팅방인지 확인
        val isAlreadyPinned = currentPinned.contains(userId)

        // 이미 즐겨찾기된 채팅방을 다시 즐겨찾기하려고 하면 제거 (토글 동작)
        if (isFavorite && isAlreadyPinned) {
            currentPinned.remove(userId)
        } 
        // 즐겨찾기 추가 요청이고 아직 즐겨찾기되지 않은 경우
        else if (isFavorite && !isAlreadyPinned) {
            if (pinnedRooms.size >= MAX_PINNED) {
                throw ApiException(
                    "최대 핀 채팅방 개수를 초과했습니다. (MAX_PINNED=$MAX_PINNED)",
                    ErrorCode.FAVORITE_LIMIT_EXCEEDED
                )
            }
            currentPinned.add(userId)
        } 
        // 즐겨찾기 해제 요청
        else if (!isFavorite) {
            currentPinned.remove(userId)
        }

        return currentPinned
    }

}
