package com.stark.shoot.adapter.out.persistence.postgres.adapter.user.friend.relate

import com.stark.shoot.adapter.out.persistence.postgres.entity.FriendshipMappingEntity
import com.stark.shoot.adapter.out.persistence.postgres.repository.FriendshipMappingRepository
import com.stark.shoot.application.port.out.user.friend.relate.FriendshipQueryPort
import com.stark.shoot.domain.user.Friendship
import com.stark.shoot.domain.user.vo.FriendshipId
import com.stark.shoot.domain.user.vo.UserId
import com.stark.shoot.infrastructure.annotation.Adapter

@Adapter
class FriendshipQueryAdapter(
    private val friendshipMappingRepository: FriendshipMappingRepository,
) : FriendshipQueryPort {

    override fun findAllFriendships(
        userId: UserId
    ): List<Friendship> {
        return friendshipMappingRepository.findAllByUserId(userId.value)
            .map { mapToDomain(it) }
    }

    override fun isFriend(
        userId: UserId,
        friendId: UserId
    ): Boolean {
        return friendshipMappingRepository.existsByUserIdAndFriendId(userId.value, friendId.value)
    }

    private fun mapToDomain(
        entity: FriendshipMappingEntity
    ): Friendship {
        return Friendship(
            id = entity.id?.let { FriendshipId.from(it) },
            userId = UserId.from(entity.user.id),
            friendId = UserId.from(entity.friend.id),
            createdAt = entity.createdAt
        )
    }

}
