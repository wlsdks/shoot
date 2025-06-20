package com.stark.shoot.adapter.out.persistence.postgres.mapper

import com.stark.shoot.adapter.out.persistence.postgres.entity.FriendRequestEntity
import com.stark.shoot.adapter.out.persistence.postgres.entity.UserEntity
import com.stark.shoot.domain.user.FriendRequest
import org.springframework.stereotype.Component

@Component
class FriendRequestMapper {
    fun toEntity(domain: FriendRequest, sender: UserEntity, receiver: UserEntity): FriendRequestEntity {
        return FriendRequestEntity(
            sender = sender,
            receiver = receiver,
            status = domain.status,
            requestDate = domain.createdAt,
            respondedAt = domain.respondedAt
        )
    }

    fun toDomain(entity: FriendRequestEntity): FriendRequest {
        return FriendRequest(
            id = entity.id,
            senderId = entity.sender.id!!,
            receiverId = entity.receiver.id!!,
            status = entity.status,
            createdAt = entity.requestDate,
            respondedAt = entity.respondedAt
        )
    }
}
