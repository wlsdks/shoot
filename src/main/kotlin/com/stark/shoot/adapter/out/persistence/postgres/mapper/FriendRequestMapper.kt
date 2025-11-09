package com.stark.shoot.adapter.out.persistence.postgres.mapper

import com.stark.shoot.adapter.out.persistence.postgres.entity.FriendRequestEntity
import com.stark.shoot.domain.social.FriendRequest
import com.stark.shoot.domain.social.vo.FriendRequestId
import com.stark.shoot.domain.shared.UserId
import org.springframework.stereotype.Component

@Component
class FriendRequestMapper {

    fun toEntity(domain: FriendRequest): FriendRequestEntity {
        return FriendRequestEntity(
            senderId = domain.senderId.value,
            receiverId = domain.receiverId.value,
            status = domain.status,
            requestDate = domain.createdAt,
            respondedAt = domain.respondedAt
        )
    }

    fun toDomain(entity: FriendRequestEntity): FriendRequest {
        return FriendRequest(
            id = entity.id?.let { FriendRequestId.from(it) },
            senderId = UserId.from(entity.senderId),
            receiverId = UserId.from(entity.receiverId),
            status = entity.status,
            createdAt = entity.requestDate,
            respondedAt = entity.respondedAt
        )
    }

}
