package com.stark.shoot.domain.notification

enum class SourceType(
    val description: String = "",
    val icon: String = ""
) {
    // Chat-related sources
    CHAT(
        description = "채팅",
        icon = "chat"
    ),
    CHAT_ROOM(
        description = "채팅방",
        icon = "chat_room"
    ),

    // User-related sources
    USER(
        description = "사용자",
        icon = "user"
    ),
    FRIEND(
        description = "친구",
        icon = "friend"
    ),

    // System sources
    SYSTEM(
        description = "시스템",
        icon = "system"
    ),

    // Other sources
    OTHER(
        description = "기타",
        icon = "other"
    )

}