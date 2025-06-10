package com.stark.shoot.adapter.out.persistence.postgres.repository

import com.stark.shoot.adapter.out.persistence.postgres.entity.FriendGroupEntity
import org.springframework.data.jpa.repository.JpaRepository

interface FriendGroupRepository : JpaRepository<FriendGroupEntity, Long> {
    fun findAllByOwnerId(ownerId: Long): List<FriendGroupEntity>
}
