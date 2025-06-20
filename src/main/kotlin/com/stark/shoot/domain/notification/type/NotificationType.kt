package com.stark.shoot.domain.notification.type

enum class NotificationType(
    val description: String = "",
    val icon: String = ""
) {
    // Chat-related notifications
    NEW_MESSAGE(
        description = "새로운 메시지가 도착했습니다.",
        icon = "message",
    ),
    MENTION(
        description = "메시지에서 내가 언급되었습니다.",
        icon = "mention",
    ),
    REACTION(
        description = "메시지에 반응이 달렸습니다.",
        icon = "reaction",
    ),
    PIN(
        description = "메시지가 고정되었습니다.",
        icon = "pin",
    ),

    // Friend-related notifications
    FRIEND_REQUEST(
        description = "친구 요청이 도착했습니다.",
        icon = "friend_request",
    ),
    FRIEND_ACCEPTED(
        description = "친구 요청이 수락되었습니다.",
        icon = "friend_accepted",
    ),
    FRIEND_REJECTED(
        description = "친구 요청이 거절되었습니다.",
        icon = "friend_rejected",
    ),
    FRIEND_REMOVED(
        description = "친구가 삭제되었습니다.",
        icon = "friend_removed",
    ),

    // System notifications
    SYSTEM_ANNOUNCEMENT(
        description = "시스템 공지사항",
        icon = "announcement",
    ),
    SYSTEM_MAINTENANCE(
        description = "시스템 점검",
        icon = "maintenance",
    ),

    // Other notifications
    OTHER(
        description = "기타 알림",
        icon = "other",
    )

}