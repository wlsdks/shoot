package com.stark.shoot.domain.chat.room

@JvmInline
value class ChatRoomAnnouncement private constructor(val value: String) {
    companion object {
        fun from(value: String): ChatRoomAnnouncement {
            require(value.isNotBlank()) { "채팅방 공지사항은 비어있을 수 없습니다." }
            require(value.length <= 200) { "채팅방 공지사항은 200자 이하여야 합니다." }
            return ChatRoomAnnouncement(value)
        }
    }

    override fun toString(): String = value
}
