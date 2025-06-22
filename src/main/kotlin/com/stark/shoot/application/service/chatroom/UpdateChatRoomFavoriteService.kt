package com.stark.shoot.application.service.chatroom

import com.stark.shoot.adapter.`in`.web.dto.chatroom.ChatRoomResponse
import com.stark.shoot.application.port.`in`.chatroom.UpdateChatRoomFavoriteUseCase
import com.stark.shoot.application.port.out.chatroom.ChatRoomCommandPort
import com.stark.shoot.application.port.out.chatroom.ChatRoomQueryPort
import com.stark.shoot.domain.chatroom.ChatRoom
import com.stark.shoot.domain.chatroom.vo.ChatRoomId
import com.stark.shoot.domain.user.vo.UserId
import com.stark.shoot.infrastructure.annotation.UseCase
import com.stark.shoot.infrastructure.exception.web.ResourceNotFoundException
import org.springframework.transaction.annotation.Transactional

@Transactional
@UseCase
class UpdateChatRoomFavoriteService(
    private val chatRoomQueryPort: ChatRoomQueryPort,
    private val chatRoomCommandPort: ChatRoomCommandPort
) : UpdateChatRoomFavoriteUseCase {

    /**
     * 사용자(userId)가 해당 채팅방(roomId)을 즐겨찾기(핀)로 설정하거나 해제합니다.
     */
    override fun updateFavoriteStatus(
        roomId: ChatRoomId,
        userId: UserId,
        isFavorite: Boolean
    ): ChatRoomResponse {
        // 채팅방 조회 (도메인 객체로 반환)
        val chatRoom: ChatRoom = chatRoomQueryPort.findById(roomId)
            ?: throw ResourceNotFoundException("채팅방을 찾을 수 없습니다. id=$roomId")

        // 현재 사용자가 핀 처리한 채팅방 목록 조회 (즐겨찾기 제한 체크용)
        val pinnedRooms = chatRoomQueryPort.findByUserId(userId)

        // 도메인 객체에서 즐겨찾기 상태 업데이트 (비즈니스 로직은 도메인 객체 내부에서 처리)
        val updatedChatRoom = chatRoom.updateFavoriteStatus(
            userId = userId,
            isFavorite = isFavorite,
            userPinnedRoomsCount = pinnedRooms.size
        )

        val saved = chatRoomCommandPort.save(updatedChatRoom)
        return ChatRoomResponse.from(saved, userId.value)
    }
}
