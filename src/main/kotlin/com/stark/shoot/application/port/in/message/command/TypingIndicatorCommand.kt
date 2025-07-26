package com.stark.shoot.application.port.`in`.message.command

import com.stark.shoot.adapter.`in`.rest.socket.dto.TypingIndicatorMessage

/**
 * 타이핑 인디케이터 커맨드
 * 사용자가 채팅방에서 타이핑 중인지 여부를 나타내는 커맨드
 */
data class TypingIndicatorCommand(
    val userId: Long,
    val roomId: Long,
    val isTyping: Boolean
) {
    companion object {
        /**
         * TypingIndicatorMessage DTO로부터 커맨드 객체를 생성합니다.
         */
        fun of(message: TypingIndicatorMessage): TypingIndicatorCommand {
            return TypingIndicatorCommand(
                userId = message.userId,
                roomId = message.roomId,
                isTyping = message.isTyping
            )
        }
    }
}