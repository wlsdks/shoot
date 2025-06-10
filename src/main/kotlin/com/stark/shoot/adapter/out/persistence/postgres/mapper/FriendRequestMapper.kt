package com.stark.shoot.adapter.out.persistence.postgres.mapper

import com.stark.shoot.adapter.out.persistence.postgres.entity.FriendRequestEntity
import com.stark.shoot.adapter.out.persistence.postgres.entity.UserEntity
import com.stark.shoot.adapter.out.persistence.postgres.entity.enumerate.FriendRequestStatus as PersistenceStatus
import com.stark.shoot.domain.chat.user.FriendRequest
import com.stark.shoot.domain.chat.user.FriendRequestStatus as DomainStatus
import org.springframework.stereotype.Component

@Component
class FriendRequestMapper {
    fun toEntity(domain: FriendRequest, sender: UserEntity, receiver: UserEntity): FriendRequestEntity {
        val status = when (domain.status) {
            DomainStatus.PENDING -> PersistenceStatus.PENDING
            DomainStatus.ACCEPTED -> PersistenceStatus.ACCEPTED
            DomainStatus.REJECTED -> PersistenceStatus.REJECTED
            DomainStatus.CANCELLED -> PersistenceStatus.CANCELLED
        }
        return FriendRequestEntity(
            sender = sender,
            receiver = receiver,
            status = status,
            requestDate = domain.createdAt,
            respondedAt = domain.respondedAt
        )
    }

    fun toDomain(entity: FriendRequestEntity): FriendRequest {
        val status = when (entity.status) {
            PersistenceStatus.PENDING -> DomainStatus.PENDING
            PersistenceStatus.ACCEPTED -> DomainStatus.ACCEPTED
            PersistenceStatus.REJECTED -> DomainStatus.REJECTED
            PersistenceStatus.CANCELLED -> DomainStatus.CANCELLED
        }
        return FriendRequest(
            id = entity.id,
            senderId = entity.sender.id!!,
            receiverId = entity.receiver.id!!,
            status = status,
            createdAt = entity.requestDate,
            respondedAt = entity.respondedAt
        )
    }
}
