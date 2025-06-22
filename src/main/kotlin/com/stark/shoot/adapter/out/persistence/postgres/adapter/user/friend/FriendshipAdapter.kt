package com.stark.shoot.adapter.out.persistence.postgres.adapter.user.friend

import com.stark.shoot.adapter.out.persistence.postgres.entity.FriendshipMappingEntity
import com.stark.shoot.adapter.out.persistence.postgres.repository.FriendshipMappingRepository
import com.stark.shoot.adapter.out.persistence.postgres.repository.UserRepository
import com.stark.shoot.application.port.out.user.friend.FriendshipPort
import com.stark.shoot.domain.user.Friendship
import com.stark.shoot.domain.user.vo.FriendshipId
import com.stark.shoot.domain.user.vo.UserId
import com.stark.shoot.infrastructure.annotation.Adapter
import com.stark.shoot.infrastructure.exception.web.ResourceNotFoundException

@Adapter
class FriendshipAdapter(
    private val friendshipMappingRepository: FriendshipMappingRepository,
    private val userRepository: UserRepository
) : FriendshipPort {

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

    override fun createFriendship(
        friendship: Friendship
    ): Friendship {
        val user = userRepository.findById(friendship.userId.value)
            .orElseThrow { ResourceNotFoundException("사용자를 찾을 수 없습니다: ${friendship.userId.value}") }

        val friend = userRepository.findById(friendship.friendId.value)
            .orElseThrow { ResourceNotFoundException("사용자를 찾을 수 없습니다: ${friendship.friendId.value}") }

        // 이미 친구 관계가 존재하는지 확인
        if (friendshipMappingRepository.existsByUserIdAndFriendId(friendship.userId.value, friendship.friendId.value)) {
            // 이미 친구인 경우 기존 엔티티 반환
            val existingEntity = friendshipMappingRepository.findAllByUserId(friendship.userId.value)
                .first { it.friend.id == friendship.friendId.value }

            return mapToDomain(existingEntity)
        }

        // 새로운 친구 관계 생성 및 저장
        val entity = FriendshipMappingEntity(
            user = user,
            friend = friend
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
