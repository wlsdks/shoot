package com.stark.shoot.adapter.out.persistence.postgres.repository

import com.stark.shoot.adapter.out.persistence.postgres.entity.FriendGroupMemberEntity
import org.springframework.data.jpa.repository.JpaRepository

interface FriendGroupMemberRepository : JpaRepository<FriendGroupMemberEntity, Long> {
    fun findAllByGroupId(groupId: Long): List<FriendGroupMemberEntity>
    fun deleteByGroupIdAndMemberId(groupId: Long, memberId: Long)
    fun deleteAllByGroupId(groupId: Long)
}
