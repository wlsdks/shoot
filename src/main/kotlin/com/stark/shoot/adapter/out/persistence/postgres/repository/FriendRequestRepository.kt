package com.stark.shoot.adapter.out.persistence.postgres.repository

import com.stark.shoot.adapter.out.persistence.postgres.entity.FriendRequestEntity
import org.springframework.data.jpa.repository.JpaRepository

interface FriendRequestRepository : JpaRepository<FriendRequestEntity, Long> {
    fun findAllBySenderId(senderId: Long): List<FriendRequestEntity>
    fun findAllByReceiverId(receiverId: Long): List<FriendRequestEntity>
    fun deleteBySenderIdAndReceiverId(senderId: Long, receiverId: Long)
    fun existsBySenderIdAndReceiverId(senderId: Long, receiverId: Long): Boolean
}