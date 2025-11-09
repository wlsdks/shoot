package com.stark.shoot.adapter.out.persistence.postgres.entity

import jakarta.persistence.*

@Entity
@Table(name = "friend_groups")
class FriendGroupEntity(
    @Column(name = "owner_id", nullable = false)
    val ownerId: Long,
    var name: String,
    var description: String?
) : BaseEntity()
