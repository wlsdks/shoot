package com.stark.shoot.adapter.out.persistence.postgres.repository

import com.stark.shoot.adapter.out.persistence.postgres.entity.FriendRequestEntity
import org.springframework.data.jpa.repository.JpaRepository

interface FriendRequestRepository : JpaRepository<FriendRequestEntity, Long> {
    fun findBySenderIdAndReceiverId(senderId: Long, receiverId: Long): FriendRequestEntity?
    fun findByReceiverId(receiverId: Long): List<FriendRequestEntity>
    fun deleteBySenderIdAndReceiverId(senderId: Long, receiverId: Long): Int
}