package com.stark.shoot.adapter.out.persistence.postgres.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table

// 친구로 맺어진 관계를 관리합니다.
@Entity
@Table(name = "friendship_map")
class FriendshipMappingEntity(
    @Column(name = "user_id", nullable = false)
    val userId: Long,

    @Column(name = "friend_id", nullable = false)
    val friendId: Long
) : BaseEntity()
