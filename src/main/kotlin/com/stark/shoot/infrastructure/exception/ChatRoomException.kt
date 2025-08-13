package com.stark.shoot.infrastructure.exception

/**
 * 채팅방 관련 도메인 예외
 */
sealed class ChatRoomException(
    message: String,
    errorCode: String,
    cause: Throwable? = null
) : DomainException(message, errorCode, cause) {

    /**
     * 채팅방을 찾을 수 없을 때 발생하는 예외
     */
    class NotFound(
        chatRoomId: Long,
        message: String = "채팅방을 찾을 수 없습니다: $chatRoomId"
    ) : ChatRoomException(message, "CHATROOM_NOT_FOUND")

    /**
     * 채팅방 ID가 없을 때 발생하는 예외
     */
    class MissingId(
        message: String = "채팅방 ID가 없습니다."
    ) : ChatRoomException(message, "CHATROOM_ID_MISSING")

    /**
     * 자기 자신과 채팅방을 만들려고 할 때 발생하는 예외
     */
    class SelfChatNotAllowed(
        message: String = "자기 자신과는 채팅방을 만들 수 없습니다."
    ) : ChatRoomException(message, "SELF_CHAT_NOT_ALLOWED")

    /**
     * 그룹 채팅방이 아닐 때 발생하는 예외
     */
    class NotGroupChat(
        message: String = "그룹 채팅방이 아닙니다."
    ) : ChatRoomException(message, "NOT_GROUP_CHAT")

    /**
     * 채팅방 참여자가 아닐 때 발생하는 예외
     */
    class NotParticipant(
        message: String = "채팅방 참여자가 아닙니다."
    ) : ChatRoomException(message, "NOT_PARTICIPANT")

    /**
     * 동일한 참여자로 구성된 그룹 채팅방이 이미 존재할 때 발생하는 예외
     */
    class DuplicateGroupChat(
        message: String = "동일한 참여자로 구성된 그룹 채팅방이 이미 존재합니다."
    ) : ChatRoomException(message, "DUPLICATE_GROUP_CHAT")

    /**
     * 채팅방 생성자가 참여자에 포함되지 않았을 때 발생하는 예외
     */
    class CreatorNotInParticipants(
        message: String = "채팅방 생성자는 참여자에 포함되어야 합니다."
    ) : ChatRoomException(message, "CREATOR_NOT_IN_PARTICIPANTS")
}