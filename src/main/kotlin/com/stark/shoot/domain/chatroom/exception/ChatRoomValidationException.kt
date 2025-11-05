package com.stark.shoot.domain.chatroom.exception

import com.stark.shoot.domain.shared.exception.DomainException

/**
 * 채팅방 검증 관련 도메인 예외
 *
 * DDD 개선: ValidationException에서 ChatRoom Context로 이동
 * ChatRoom Context의 참여자 수 검증 규칙 위반 시 발생
 */
sealed class ChatRoomValidationException(
    message: String,
    errorCode: String,
    cause: Throwable? = null
) : DomainException(message, errorCode, cause) {

    /**
     * 그룹 채팅방 최소 참여자 수 미달일 때 발생하는 예외
     */
    class GroupChatMinParticipants(
        minParticipants: Int,
        message: String = "그룹 채팅방은 최소 ${minParticipants}명의 참여자가 필요합니다."
    ) : ChatRoomValidationException(message, "GROUP_CHAT_MIN_PARTICIPANTS")

    /**
     * 그룹 채팅방 최대 참여자 수 초과일 때 발생하는 예외
     */
    class GroupChatMaxParticipants(
        maxParticipants: Int,
        message: String = "그룹 채팅방은 최대 ${maxParticipants}명까지 참여할 수 있습니다."
    ) : ChatRoomValidationException(message, "GROUP_CHAT_MAX_PARTICIPANTS")
}
