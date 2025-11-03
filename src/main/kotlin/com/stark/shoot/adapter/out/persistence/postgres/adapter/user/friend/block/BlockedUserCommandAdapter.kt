package com.stark.shoot.adapter.out.persistence.postgres.adapter.user.friend.block

import com.stark.shoot.adapter.out.persistence.postgres.entity.BlockedUserEntity
import com.stark.shoot.adapter.out.persistence.postgres.repository.BlockedUserRepository
import com.stark.shoot.adapter.out.persistence.postgres.repository.UserRepository
import com.stark.shoot.application.port.out.user.block.BlockedUserCommandPort
import com.stark.shoot.domain.social.BlockedUser
import com.stark.shoot.domain.user.vo.UserId
import com.stark.shoot.infrastructure.annotation.Adapter

@Adapter
class BlockedUserCommandAdapter(
    private val blockedUserRepository: BlockedUserRepository,
    private val userRepository: UserRepository
) : BlockedUserCommandPort {

    override fun blockUser(
        blockedUser: BlockedUser
    ): BlockedUser {
        // UserBlockService에서 이미 사용자 존재 여부를 확인했으므로 여기서는 존재한다고 가정
        val user = userRepository.getReferenceById(blockedUser.userId.value)
        val blockedUserEntity = userRepository.getReferenceById(blockedUser.blockedUserId.value)

        // 새로운 차단 관계 생성 및 저장
        val entity = BlockedUserEntity(
            user = user,
            blockedUser = blockedUserEntity,
            blockedAt = blockedUser.createdAt
        )

        val savedEntity = blockedUserRepository.save(entity)
        return mapToDomain(savedEntity)
    }

    override fun unblockUser(
        userId: UserId,
        blockedUserId: UserId
    ) {
        blockedUserRepository.deleteByUserIdAndBlockedUserId(userId.value, blockedUserId.value)
    }

    private fun mapToDomain(
        entity: BlockedUserEntity
    ): BlockedUser {
        return BlockedUser(
            id = entity.id,
            userId = UserId.Companion.from(entity.user.id),
            blockedUserId = UserId.Companion.from(entity.blockedUser.id),
            createdAt = entity.blockedAt
        )
    }

}