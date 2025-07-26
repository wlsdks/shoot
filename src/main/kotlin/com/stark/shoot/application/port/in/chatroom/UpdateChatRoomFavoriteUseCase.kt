package com.stark.shoot.application.port.`in`.chatroom

import com.stark.shoot.adapter.`in`.rest.dto.chatroom.ChatRoomResponse
import com.stark.shoot.application.port.`in`.chatroom.command.UpdateFavoriteStatusCommand

interface UpdateChatRoomFavoriteUseCase {
    /**
     * 채팅방 즐겨찾기 상태를 업데이트합니다.
     *
     * @param command 즐겨찾기 상태 업데이트 커맨드
     * @return 업데이트된 채팅방 정보
     */
    fun updateFavoriteStatus(command: UpdateFavoriteStatusCommand): ChatRoomResponse
}
