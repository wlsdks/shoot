package com.stark.shoot.adapter.out.persistence.postgres.entity

import jakarta.persistence.*
import java.time.Instant

/**
 * 채팅방 즐겨찾기 JPA Entity
 *
 * 사용자의 채팅방 즐겨찾기/고정 설정을 저장합니다.
 */
@Entity
@Table(
    name = "chat_room_favorites",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_chat_room_favorite_user_room",
            columnNames = ["user_id", "chat_room_id"]
        )
    ],
    indexes = [
        Index(name = "idx_chat_room_favorite_user_id", columnList = "user_id"),
        Index(name = "idx_chat_room_favorite_chat_room_id", columnList = "chat_room_id"),
        Index(name = "idx_chat_room_favorite_user_pinned", columnList = "user_id, is_pinned")
    ]
)
class ChatRoomFavoriteEntity(
    @Column(name = "user_id", nullable = false)
    val userId: Long,

    @Column(name = "chat_room_id", nullable = false)
    val chatRoomId: Long,

    @Column(name = "is_pinned", nullable = false)
    var isPinned: Boolean = true,

    @Column(name = "pinned_at", nullable = false)
    var pinnedAt: Instant = Instant.now(),

    @Column(name = "display_order")
    var displayOrder: Int? = null
) : BaseEntity() {

    fun update(
        isPinned: Boolean = this.isPinned,
        pinnedAt: Instant = this.pinnedAt,
        displayOrder: Int? = this.displayOrder
    ) {
        this.isPinned = isPinned
        this.pinnedAt = pinnedAt
        this.displayOrder = displayOrder
    }
}
