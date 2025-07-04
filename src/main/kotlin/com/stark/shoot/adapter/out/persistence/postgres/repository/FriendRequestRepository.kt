package com.stark.shoot.adapter.out.persistence.postgres.repository

import com.stark.shoot.adapter.out.persistence.postgres.entity.FriendRequestEntity
import com.stark.shoot.domain.user.type.FriendRequestStatus
import org.springframework.data.jpa.repository.JpaRepository

interface FriendRequestRepository : JpaRepository<FriendRequestEntity, Long> {
    fun findAllBySenderIdAndStatus(senderId: Long, status: FriendRequestStatus): List<FriendRequestEntity>
    fun findAllByReceiverIdAndStatus(receiverId: Long, status: FriendRequestStatus): List<FriendRequestEntity>
    fun findBySenderIdAndReceiverId(senderId: Long, receiverId: Long): FriendRequestEntity?
    fun findAllBySenderIdAndReceiverId(senderId: Long, receiverId: Long): List<FriendRequestEntity>
    fun existsBySenderIdAndReceiverIdAndStatus(senderId: Long, receiverId: Long, status: FriendRequestStatus): Boolean
}
