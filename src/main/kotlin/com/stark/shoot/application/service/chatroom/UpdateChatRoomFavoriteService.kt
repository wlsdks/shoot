package com.stark.shoot.application.service.chatroom

import com.stark.shoot.adapter.`in`.rest.dto.chatroom.ChatRoomResponse
import com.stark.shoot.application.port.`in`.chatroom.UpdateChatRoomFavoriteUseCase
import com.stark.shoot.application.port.`in`.chatroom.command.UpdateFavoriteStatusCommand
import com.stark.shoot.application.port.out.chatroom.ChatRoomQueryPort
import com.stark.shoot.application.port.out.chatroom.favorite.ChatRoomFavoriteCommandPort
import com.stark.shoot.application.port.out.chatroom.favorite.ChatRoomFavoriteQueryPort
import com.stark.shoot.domain.chatroom.ChatRoom
import com.stark.shoot.domain.chatroom.constants.ChatRoomConstants
import com.stark.shoot.domain.chatroom.exception.FavoriteLimitExceededException
import com.stark.shoot.domain.chatroom.favorite.ChatRoomFavorite
import com.stark.shoot.infrastructure.annotation.UseCase
import com.stark.shoot.infrastructure.exception.web.ResourceNotFoundException
import org.springframework.transaction.annotation.Transactional

@Transactional
@UseCase
class UpdateChatRoomFavoriteService(
    private val chatRoomQueryPort: ChatRoomQueryPort,
    private val favoriteCommandPort: ChatRoomFavoriteCommandPort,
    private val favoriteQueryPort: ChatRoomFavoriteQueryPort,
    private val chatRoomConstants: ChatRoomConstants
) : UpdateChatRoomFavoriteUseCase {

    /**
     * 채팅방 즐겨찾기 상태를 업데이트합니다.
     *
     * DDD 개선: ChatRoomFavorite Aggregate 사용
     * - 사용자의 개인 설정으로 분리되어 동시성 충돌 제거
     * - 각 사용자가 독립적으로 즐겨찾기 관리 가능
     *
     * @param command 즐겨찾기 상태 업데이트 커맨드
     * @return 업데이트된 채팅방 정보
     */
    override fun updateFavoriteStatus(command: UpdateFavoriteStatusCommand): ChatRoomResponse {
        val roomId = command.roomId
        val userId = command.userId
        val isFavorite = command.isFavorite

        // 채팅방 존재 여부 확인
        val chatRoom: ChatRoom = chatRoomQueryPort.findById(roomId)
            ?: throw ResourceNotFoundException("채팅방을 찾을 수 없습니다. id=$roomId")

        // 기존 즐겨찾기 조회
        val existingFavorite = favoriteQueryPort.findByUserIdAndChatRoomId(userId, roomId)

        if (isFavorite) {
            // 즐겨찾기 추가/재활성화
            if (existingFavorite == null) {
                // 신규 즐겨찾기 생성
                val pinnedCount = favoriteQueryPort.countPinnedByUserId(userId)
                if (pinnedCount >= chatRoomConstants.maxPinnedChatRooms) {
                    throw FavoriteLimitExceededException(
                        "최대 핀 채팅방 개수를 초과했습니다. (최대: ${chatRoomConstants.maxPinnedChatRooms}개)"
                    )
                }

                val newFavorite = ChatRoomFavorite.create(userId, roomId)
                favoriteCommandPort.save(newFavorite)
            } else if (!existingFavorite.isPinned) {
                // 기존 즐겨찾기 재활성화 (unpin -> pin)
                val pinnedCount = favoriteQueryPort.countPinnedByUserId(userId)
                if (pinnedCount >= chatRoomConstants.maxPinnedChatRooms) {
                    throw FavoriteLimitExceededException(
                        "최대 핀 채팅방 개수를 초과했습니다. (최대: ${chatRoomConstants.maxPinnedChatRooms}개)"
                    )
                }

                existingFavorite.repin()
                favoriteCommandPort.save(existingFavorite)
            }
            // 이미 즐겨찾기되어 있으면 아무 작업도 하지 않음
        } else {
            // 즐겨찾기 해제
            if (existingFavorite != null) {
                if (existingFavorite.isPinned) {
                    existingFavorite.unpin()
                    favoriteCommandPort.save(existingFavorite)
                }
                // 완전히 삭제하거나 unpin 상태로 유지
                // 현재는 unpin 상태로 유지 (히스토리 보존)
            }
        }

        // 최종 즐겨찾기 상태 조회
        val finalFavorite = favoriteQueryPort.findByUserIdAndChatRoomId(userId, roomId)
        val finalIsPinned = finalFavorite?.isPinned ?: false

        return ChatRoomResponse.from(chatRoom, userId.value, finalIsPinned)
    }
}
