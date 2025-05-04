package com.stark.shoot.application.service.message

import com.stark.shoot.application.port.`in`.chatroom.CreateChatRoomUseCase
import com.stark.shoot.application.port.`in`.message.ForwardMessageToUserUseCase
import com.stark.shoot.application.port.`in`.message.ForwardMessageUseCase
import com.stark.shoot.domain.chat.message.ChatMessage
import com.stark.shoot.infrastructure.annotation.UseCase

@UseCase
class ForwardMessageToUserService(
    private val createChatRoomUseCase: CreateChatRoomUseCase,
    private val forwardMessageUseCase: ForwardMessageUseCase
) : ForwardMessageToUserUseCase {

    /**
     * 메시지를 특정 사용자(친구)에게 전달합니다.
     * 1:1 채팅방을 생성하거나 조회한 후, 해당 채팅방으로 메시지를 전달합니다.
     */
    override fun forwardMessageToUser(
        originalMessageId: String,
        targetUserId: Long,
        forwardingUserId: Long
    ): ChatMessage {
        // 1. 사용자 간 1:1 채팅방 생성 또는 조회
        val chatRoom = createChatRoomUseCase.createDirectChat(
            userId = forwardingUserId,
            friendId = targetUserId
        )

        // 채팅방 ID가 null인 경우 예외 처리
        val roomId = chatRoom.id ?: throw IllegalStateException("채팅방 ID가 null입니다. 메시지를 전달할 수 없습니다.")

        // 2. 해당 채팅방으로 메시지 전달
        return forwardMessageUseCase.forwardMessage(
            originalMessageId = originalMessageId,
            targetRoomId = roomId,
            forwardingUserId = forwardingUserId
        )
    }
}