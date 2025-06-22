package com.stark.shoot.adapter.out.persistence.postgres.repository

import com.stark.shoot.adapter.out.persistence.postgres.entity.BlockedUserEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface BlockedUserRepository : JpaRepository<BlockedUserEntity, Long> {
    fun findAllByUserId(userId: Long): List<BlockedUserEntity>
    fun findAllByBlockedUserId(blockedUserId: Long): List<BlockedUserEntity>
    fun existsByUserIdAndBlockedUserId(userId: Long, blockedUserId: Long): Boolean
    fun deleteByUserIdAndBlockedUserId(userId: Long, blockedUserId: Long)
}