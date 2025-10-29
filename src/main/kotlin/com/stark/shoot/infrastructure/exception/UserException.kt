package com.stark.shoot.infrastructure.exception

/**
 * 사용자 관련 도메인 예외
 */
sealed class UserException(
    message: String,
    errorCode: String,
    cause: Throwable? = null
) : DomainException(message, errorCode, cause) {

    /**
     * 존재하지 않는 사용자일 때 발생하는 예외
     */
    class NotFound(
        userId: Long,
        message: String = "존재하지 않는 사용자입니다: $userId"
    ) : UserException(message, "USER_NOT_FOUND")

    /**
     * 자기 자신에게 친구 요청을 보내려고 할 때 발생하는 예외
     */
    class SelfFriendRequestNotAllowed(
        message: String = "자기 자신에게 친구 요청을 보낼 수 없습니다."
    ) : UserException(message, "SELF_FRIEND_REQUEST_NOT_ALLOWED")

    /**
     * 이미 친구 상태일 때 발생하는 예외
     */
    class AlreadyFriends(
        message: String = "이미 친구 상태입니다."
    ) : UserException(message, "ALREADY_FRIENDS")

    /**
     * 이미 친구 요청을 보냈을 때 발생하는 예외
     */
    class FriendRequestAlreadySent(
        message: String = "이미 친구 요청을 보냈습니다."
    ) : UserException(message, "FRIEND_REQUEST_ALREADY_SENT")

    /**
     * 상대방이 이미 친구 요청을 보냈을 때 발생하는 예외
     */
    class FriendRequestAlreadyReceived(
        message: String = "상대방이 이미 친구 요청을 보냈습니다. 수락하거나 거절해주세요."
    ) : UserException(message, "FRIEND_REQUEST_ALREADY_RECEIVED")

    /**
     * 이미 참여 중인 사용자일 때 발생하는 예외
     */
    class AlreadyParticipant(
        userId: Long,
        message: String = "이미 참여 중인 사용자입니다: $userId"
    ) : UserException(message, "ALREADY_PARTICIPANT")

    /**
     * 참여하지 않은 사용자일 때 발생하는 예외
     */
    class NotParticipant(
        userId: Long,
        message: String = "참여하지 않은 사용자입니다: $userId"
    ) : UserException(message, "NOT_PARTICIPANT")

    /**
     * 인증 정보가 올바르지 않을 때 발생하는 예외 (비밀번호 불일치 등)
     */
    class InvalidCredentials(
        message: String = "인증 정보가 올바르지 않습니다."
    ) : UserException(message, "INVALID_CREDENTIALS")
}