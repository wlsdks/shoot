package com.stark.shoot.application.service.chatroom

import com.stark.shoot.adapter.`in`.web.dto.chatroom.ChatRoomResponse
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

    /**
     * 사용자(userId)가 해당 채팅방(roomId)을 즐겨찾기(핀)로 설정하거나 해제합니다.
     */
    override fun updateFavoriteStatus(
        roomId: Long,
        userId: Long,
        isFavorite: Boolean
    ): ChatRoomResponse {
        // 채팅방 조회 (도메인 객체로 반환)
        val chatRoom: ChatRoom = loadChatRoomPort.findById(roomId)
            ?: throw ResourceNotFoundException("채팅방을 찾을 수 없습니다. id=$roomId")

        // 현재 사용자가 핀 처리한 채팅방 목록 조회 (즐겨찾기 제한 체크용)
        val pinnedRooms = loadPinnedRoomsPort.findByUserId(userId)

        // 도메인 객체에서 즐겨찾기 상태 업데이트 (비즈니스 로직은 도메인 객체 내부에서 처리)
        val updatedChatRoom = chatRoom.updateFavoriteStatus(
            userId = userId,
            isFavorite = isFavorite,
            userPinnedRoomsCount = pinnedRooms.size
        )

        val saved = saveChatRoomPort.save(updatedChatRoom)
        return ChatRoomResponse.from(saved, userId)
    }
}
