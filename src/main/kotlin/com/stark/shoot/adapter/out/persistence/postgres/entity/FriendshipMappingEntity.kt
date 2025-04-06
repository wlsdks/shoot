package com.stark.shoot.adapter.out.persistence.postgres.entity

import jakarta.persistence.Entity
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

// 친구로 맺어진 관계를 관리합니다.
@Entity
@Table(name = "friendship_map")
class FriendshipMappingEntity(
    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    val user: UserEntity,

    @ManyToOne(optional = false)
    @JoinColumn(name = "friend_id", referencedColumnName = "id")
    val friend: UserEntity
) : BaseEntity()
