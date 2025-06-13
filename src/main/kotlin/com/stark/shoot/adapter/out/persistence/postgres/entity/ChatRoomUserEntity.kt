package com.stark.shoot.adapter.out.persistence.postgres.entity

import com.stark.shoot.adapter.out.persistence.postgres.entity.enumerate.ChatRoomUserRole
import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(
    name = "chat_room_users",
    uniqueConstraints = [UniqueConstraint(columnNames = ["chat_room_id", "user_id"])]
)
class ChatRoomUserEntity(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_room_id", nullable = false)
    val chatRoom: ChatRoomEntity,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: UserEntity,

    @Column(name = "is_pinned")
    var isPinned: Boolean = false,

    @Column(name = "last_read_message_mongodb_id", length = 64)
    var lastReadMessageId: String? = null,  // MongoDB의 ObjectId를 문자열로 저장

    @Column(name = "joined_at", nullable = false)
    val joinedAt: Instant = Instant.now(),

    @Column(name = "role")
    @Enumerated(EnumType.STRING)
    var role: ChatRoomUserRole = ChatRoomUserRole.MEMBER
) : BaseEntity()
