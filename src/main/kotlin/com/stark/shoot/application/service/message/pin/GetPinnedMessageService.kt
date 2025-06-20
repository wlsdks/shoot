package com.stark.shoot.application.service.message.pin

import com.stark.shoot.application.port.`in`.message.pin.GetPinnedMessageUseCase
import com.stark.shoot.application.port.out.message.LoadMessagePort
import com.stark.shoot.domain.chat.message.ChatMessage
import com.stark.shoot.domain.chat.room.vo.ChatRoomId
import com.stark.shoot.infrastructure.annotation.UseCase

@UseCase
class GetPinnedMessageService(
    private val loadMessagePort: LoadMessagePort,
) : GetPinnedMessageUseCase {

    /**
     * 채팅방에서 고정된 메시지를 조회합니다.
     * 한 채팅방에는 최대 1개의 고정 메시지만 존재합니다.
     *
     * @param roomId 채팅방 ID
     * @return 고정된 메시지 목록 (최대 1개)
     */
    override fun getPinnedMessages(
        roomId: ChatRoomId,
    ): List<ChatMessage> {
        // 채팅방에서 고정된 메시지 조회 (최대 1개)
        return loadMessagePort.findPinnedMessagesByRoomId(roomId, 1)
    }

}