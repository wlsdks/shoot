package com.stark.shoot.adapter.out.persistence.postgres.repository

import com.stark.shoot.adapter.out.persistence.postgres.entity.FriendshipMappingEntity
import org.springframework.data.jpa.repository.JpaRepository

interface FriendshipMappingRepository : JpaRepository<FriendshipMappingEntity, Long> {
    fun findByUserIdAndFriendId(userId: Long, friendId: Long): FriendshipMappingEntity?
    fun findByUserId(userId: Long): List<FriendshipMappingEntity>
    fun deleteByUserIdAndFriendId(userId: Long, friendId: Long): Int
}