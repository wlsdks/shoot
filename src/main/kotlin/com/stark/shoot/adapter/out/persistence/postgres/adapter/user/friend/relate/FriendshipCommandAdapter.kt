package com.stark.shoot.adapter.out.persistence.postgres.adapter.user.friend.relate

import com.stark.shoot.adapter.out.persistence.postgres.entity.FriendshipMappingEntity
import com.stark.shoot.adapter.out.persistence.postgres.repository.FriendshipMappingRepository
import com.stark.shoot.application.port.out.user.friend.relate.FriendshipCommandPort
import com.stark.shoot.domain.social.Friendship
import com.stark.shoot.domain.social.vo.FriendshipId
import com.stark.shoot.domain.shared.UserId
import com.stark.shoot.infrastructure.annotation.Adapter

@Adapter
class FriendshipCommandAdapter(
    private val friendshipMappingRepository: FriendshipMappingRepository
) : FriendshipCommandPort {

    override fun createFriendship(
        friendship: Friendship
    ): Friendship {
        // 이미 친구 관계가 존재하는지 확인
        if (friendshipMappingRepository.existsByUserIdAndFriendId(friendship.userId.value, friendship.friendId.value)) {
            // 이미 친구인 경우 기존 엔티티 반환
            val existingEntity = friendshipMappingRepository.findAllByUserId(friendship.userId.value)
                .first { it.friendId == friendship.friendId.value }

            return mapToDomain(existingEntity)
        }

        // 새로운 친구 관계 생성 및 저장
        val entity = FriendshipMappingEntity(
            userId = friendship.userId.value,
            friendId = friendship.friendId.value
        )
        val savedEntity = friendshipMappingRepository.save(entity)
        return mapToDomain(savedEntity)
    }

    override fun removeFriendship(
        userId: UserId,
        friendId: UserId
    ) {
        friendshipMappingRepository.deleteByUserIdAndFriendId(userId.value, friendId.value)
    }

    /**
     * 양방향 친구 관계를 원자적으로 생성
     * 단일 쿼리로 A→B와 B→A 관계를 동시에 생성하여 데이터 정합성 보장
     */
    override fun createBidirectionalFriendship(userId1: UserId, userId2: UserId) {
        friendshipMappingRepository.createBidirectional(userId1.value, userId2.value)
    }

    /**
     * 양방향 친구 관계를 원자적으로 삭제
     * 단일 쿼리로 A→B와 B→A 관계를 동시에 삭제하여 데이터 정합성 보장
     */
    override fun removeBidirectionalFriendship(userId1: UserId, userId2: UserId) {
        friendshipMappingRepository.deleteBidirectional(userId1.value, userId2.value)
    }

    private fun mapToDomain(
        entity: FriendshipMappingEntity
    ): Friendship {
        return Friendship(
            id = entity.id?.let { FriendshipId.from(it) },
            userId = UserId.from(entity.userId),
            friendId = UserId.from(entity.friendId),
            createdAt = entity.createdAt
        )
    }

}
