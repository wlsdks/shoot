package com.stark.shoot.domain.chatroom.favorite.vo

import com.stark.shoot.infrastructure.annotation.ValueObject

/**
 * 채팅방 즐겨찾기 ID Value Object
 * Long 타입의 ID를 감싸는 값 객체입니다.
 */
@ValueObject
@JvmInline
value class ChatRoomFavoriteId(val value: Long) {
    companion object {
        fun from(value: Long): ChatRoomFavoriteId = ChatRoomFavoriteId(value)
    }
}
