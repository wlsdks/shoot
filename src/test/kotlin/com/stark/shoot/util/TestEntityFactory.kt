package com.stark.shoot.util

import com.stark.shoot.adapter.out.persistence.postgres.entity.ChatRoomEntity
import com.stark.shoot.adapter.out.persistence.postgres.entity.ChatRoomUserEntity
import com.stark.shoot.adapter.out.persistence.postgres.entity.UserEntity
import com.stark.shoot.adapter.out.persistence.postgres.entity.enumerate.ChatRoomUserRole
import com.stark.shoot.domain.chatroom.ChatRoom
import com.stark.shoot.domain.chatroom.type.ChatRoomType
import com.stark.shoot.domain.chatroom.vo.ChatRoomTitle
import com.stark.shoot.domain.user.type.UserStatus
import com.stark.shoot.domain.user.vo.UserId
import java.time.Instant

object TestEntityFactory {
    fun createUser(username: String, userCode: String): UserEntity {
        return UserEntity(
            username = username,
            nickname = username,
            status = UserStatus.ONLINE,
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
        lastReadMessageId: String,
        isPinned: Boolean = false,
        role: ChatRoomUserRole = ChatRoomUserRole.MEMBER,
    ): ChatRoomUserEntity {
        return ChatRoomUserEntity(room, user, isPinned, role = role, lastReadMessageId = lastReadMessageId)
    }

    fun createChatRoomDomain(
        participants: MutableSet<Long>,
        pinned: MutableSet<Long> = mutableSetOf(),
        type: ChatRoomType = ChatRoomType.GROUP,
        title: String? = null
    ): ChatRoom {
        return ChatRoom(
            type = type,
            participants = participants.map { UserId.from(it) }.toMutableSet(),
            pinnedParticipants = pinned.map { UserId.from(it) }.toMutableSet(),
            title = title?.let { ChatRoomTitle.from(it) },
            announcement = null,
            lastMessageId = null,
            lastActiveAt = Instant.now(),
            createdAt = Instant.now()
        )
    }
}
