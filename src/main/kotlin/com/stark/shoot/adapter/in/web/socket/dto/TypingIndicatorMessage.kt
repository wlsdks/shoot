package com.stark.shoot.adapter.`in`.web.socket.dto

/**
 * 클라이언트가 타이핑 상태를 전송할 때 사용하는 메시지 모델.
 *
 * roomId: 타이핑 이벤트가 발생한 채팅방 ID
 * userId: 타이핑 중인 사용자 ID
 * isTyping: true이면 타이핑 시작, false이면 타이핑 종료를 의미
 */
data class TypingIndicatorMessage(
    val roomId: Long,
    val userId: Long,
    val username: String? = null, // nullable로 변경
    val isTyping: Boolean
)