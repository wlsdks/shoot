package com.stark.shoot.application.service.message

import com.stark.shoot.adapter.`in`.web.dto.chatroom.ChatRoomResponse
import com.stark.shoot.application.port.`in`.chatroom.CreateChatRoomUseCase
import com.stark.shoot.application.port.`in`.message.ForwardMessageToUserUseCase
import com.stark.shoot.application.port.`in`.message.ForwardMessageUseCase
import com.stark.shoot.domain.chat.message.ChatMessage
import com.stark.shoot.domain.chatroom.vo.ChatRoomId
import com.stark.shoot.domain.chat.message.vo.MessageId
import com.stark.shoot.domain.user.vo.UserId
import com.stark.shoot.infrastructure.annotation.UseCase

// fixme: 이 클래스는 서비스에서 서비스를 호출하므로 리팩토링이 필요함
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
        originalMessageId: MessageId,
        targetUserId: UserId,
        forwardingUserId: UserId
    ): ChatMessage {
        // 1. 사용자 간 1:1 채팅방 생성 또는 조회
        val chatRoom: ChatRoomResponse = createChatRoomUseCase.createDirectChat(
            userId = forwardingUserId,
            friendId = targetUserId
        )

        val roomId = chatRoom.roomId.takeIf { it > 0 }
            ?: throw IllegalStateException("채팅방 ID가 null입니다.")

        // 2. 해당 채팅방으로 메시지 전달
        return forwardMessageUseCase.forwardMessage(
            originalMessageId = originalMessageId,
            targetRoomId = ChatRoomId.from(roomId),
            forwardingUserId = forwardingUserId
        )
    }

}