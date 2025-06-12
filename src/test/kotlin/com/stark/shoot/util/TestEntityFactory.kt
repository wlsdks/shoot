package com.stark.shoot.util

import com.stark.shoot.adapter.out.persistence.postgres.entity.ChatRoomEntity
import com.stark.shoot.adapter.out.persistence.postgres.entity.ChatRoomUserEntity
import com.stark.shoot.adapter.out.persistence.postgres.entity.UserEntity
import com.stark.shoot.adapter.out.persistence.postgres.entity.enumerate.ChatRoomUserRole
import com.stark.shoot.domain.chat.room.ChatRoom
import com.stark.shoot.domain.chat.room.ChatRoomType
import java.time.Instant

object TestEntityFactory {
    fun createUser(username: String, userCode: String): UserEntity {
        return UserEntity(
            username = username,
            nickname = username,
            status = com.stark.shoot.domain.chat.user.UserStatus.ACTIVE,
            userCode = userCode
        )
    }

    fun createChatRoomEntity(
        title: String?,
        type: ChatRoomType = ChatRoomType.GROUP,
        announcement: String? = null,
        lastMessageId: Long? = null,
        lastActiveAt: Instant = Instant.now()
    ): ChatRoomEntity {
        return ChatRoomEntity(title, type, announcement, lastMessageId, lastActiveAt)
    }

    fun createChatRoomUser(
        room: ChatRoomEntity,
        user: UserEntity,
        isPinned: Boolean = false,
        role: ChatRoomUserRole = ChatRoomUserRole.MEMBER
    ): ChatRoomUserEntity {
        return ChatRoomUserEntity(room, user, isPinned, role = role)
    }

    fun createChatRoomDomain(
        participants: MutableSet<Long>,
        pinned: MutableSet<Long> = mutableSetOf(),
        type: ChatRoomType = ChatRoomType.GROUP,
        title: String? = null
    ): ChatRoom {
        return ChatRoom(
            type = type,
            participants = participants,
            pinnedParticipants = pinned,
            title = title,
            announcement = null,
            lastMessageId = null,
            lastActiveAt = Instant.now(),
            createdAt = Instant.now()
        )
    }
}
