package com.stark.shoot.domain.chatroom.exception

import com.stark.shoot.domain.shared.exception.DomainException

/**
 * 채팅방 참여자 관련 도메인 예외
 *
 * DDD 개선: UserException에서 ChatRoom Context로 이동
 * ChatRoom Context의 참여자 관리 비즈니스 규칙 위반 시 발생
 */
sealed class ParticipantException(
    message: String,
    errorCode: String,
    cause: Throwable? = null
) : DomainException(message, errorCode, cause) {

    /**
     * 이미 참여 중인 사용자일 때 발생하는 예외
     */
    class AlreadyParticipant(
        userId: Long,
        message: String = "이미 참여 중인 사용자입니다: $userId"
    ) : ParticipantException(message, "ALREADY_PARTICIPANT")

    /**
     * 참여하지 않은 사용자일 때 발생하는 예외
     */
    class NotParticipant(
        userId: Long,
        message: String = "참여하지 않은 사용자입니다: $userId"
    ) : ParticipantException(message, "NOT_PARTICIPANT")
}
