package com.stark.shoot.application.port.`in`.message

import com.stark.shoot.application.port.`in`.message.command.ForwardMessageToUserCommand
import com.stark.shoot.domain.chat.message.ChatMessage

interface ForwardMessageToUserUseCase {
    /**
     * 메시지를 특정 사용자(친구)에게 전달합니다.
     * 1:1 채팅방을 생성하거나 조회한 후, 해당 채팅방으로 메시지를 전달합니다.
     *
     * @param command 메시지 전달 커맨드 (원본 메시지 ID, 대상 사용자 ID, 전달하는 사용자 ID)
     * @return 전달된 메시지
     */
    fun forwardMessageToUser(command: ForwardMessageToUserCommand): ChatMessage
}
