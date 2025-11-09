package com.stark.shoot.adapter.out.persistence.postgres.mapper

import com.stark.shoot.adapter.out.persistence.postgres.entity.FriendRequestEntity
import com.stark.shoot.adapter.out.persistence.postgres.entity.UserEntity
import com.stark.shoot.domain.social.FriendRequest
import com.stark.shoot.domain.social.vo.FriendRequestId
import com.stark.shoot.domain.shared.UserId
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
            id = entity.id?.let { FriendRequestId.from(it) },
            senderId = UserId.from(entity.sender.id),
            receiverId = UserId.from(entity.receiver.id),
            status = entity.status,
            createdAt = entity.requestDate,
            respondedAt = entity.respondedAt
        )
    }

}
