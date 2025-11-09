package com.stark.shoot.adapter.out.persistence.postgres.adapter.user.friend.block

import com.stark.shoot.adapter.out.persistence.postgres.entity.BlockedUserEntity
import com.stark.shoot.adapter.out.persistence.postgres.repository.BlockedUserRepository
import com.stark.shoot.application.port.out.user.block.BlockedUserCommandPort
import com.stark.shoot.domain.social.BlockedUser
import com.stark.shoot.domain.social.vo.BlockedUserId
import com.stark.shoot.domain.shared.UserId
import com.stark.shoot.infrastructure.annotation.Adapter

@Adapter
class BlockedUserCommandAdapter(
    private val blockedUserRepository: BlockedUserRepository
) : BlockedUserCommandPort {

    override fun blockUser(
        blockedUser: BlockedUser
    ): BlockedUser {
        // 새로운 차단 관계 생성 및 저장
        val entity = BlockedUserEntity(
            userId = blockedUser.userId.value,
            blockedUserId = blockedUser.blockedUserId.value,
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
            id = entity.id?.let { BlockedUserId.from(it) },
            userId = UserId.Companion.from(entity.userId),
            blockedUserId = UserId.Companion.from(entity.blockedUserId),
            createdAt = entity.blockedAt
        )
    }

}