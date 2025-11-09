package com.stark.shoot.adapter.out.persistence.postgres.entity

import jakarta.persistence.*

@Entity
@Table(
    name = "friend_group_members",
    uniqueConstraints = [UniqueConstraint(columnNames = ["group_id", "member_id"])]
)
class FriendGroupMemberEntity(
    @Column(name = "group_id", nullable = false)
    val groupId: Long,

    @Column(name = "member_id", nullable = false)
    val memberId: Long,
) : BaseEntity()
