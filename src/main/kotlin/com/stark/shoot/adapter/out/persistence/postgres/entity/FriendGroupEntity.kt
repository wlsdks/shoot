package com.stark.shoot.adapter.out.persistence.postgres.entity

import jakarta.persistence.*

@Entity
@Table(name = "friend_groups")
class FriendGroupEntity(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    val owner: UserEntity,
    var name: String,
    var description: String?
) : BaseEntity()
