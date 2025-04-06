package com.stark.shoot.adapter.out.persistence.postgres.entity

import com.stark.shoot.adapter.out.persistence.postgres.entity.enumerate.ChatRoomType
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "chat_rooms")
class ChatRoomEntity(
    title: String?,
    type: ChatRoomType,
    announcement: String?,
    lastMessageId: Long?,
    lastActiveAt: Instant
) : BaseEntity() {

    var title: String? = title
        protected set

    @Enumerated(EnumType.STRING)
    var type: ChatRoomType = type
        protected set

    var announcement: String? = announcement
        protected set

    var lastMessageId: Long? = lastMessageId
        protected set

    var lastActiveAt: Instant = lastActiveAt
        protected set

}
