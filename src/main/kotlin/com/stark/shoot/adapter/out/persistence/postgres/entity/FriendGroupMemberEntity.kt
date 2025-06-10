package com.stark.shoot.adapter.out.persistence.postgres.entity

import jakarta.persistence.*

@Entity
@Table(
    name = "friend_group_members",
    uniqueConstraints = [UniqueConstraint(columnNames = ["group_id", "member_id"])]
)
class FriendGroupMemberEntity(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    val group: FriendGroupEntity,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    val member: UserEntity,
) : BaseEntity()
