package com.stark.shoot.adapter.out.persistence.postgres.adapter.user.friend.block

import com.stark.shoot.adapter.out.persistence.postgres.entity.BlockedUserEntity
import com.stark.shoot.adapter.out.persistence.postgres.repository.BlockedUserRepository
import com.stark.shoot.application.port.out.user.block.BlockedUserQueryPort
import com.stark.shoot.domain.social.BlockedUser
import com.stark.shoot.domain.social.vo.BlockedUserId
import com.stark.shoot.domain.shared.UserId
import com.stark.shoot.infrastructure.annotation.Adapter

@Adapter
class BlockedUserQueryAdapter(
    private val blockedUserRepository: BlockedUserRepository,
) : BlockedUserQueryPort {

    override fun findAllBlockedUsers(
        userId: UserId
    ): List<BlockedUser> {
        return blockedUserRepository.findAllByUserId(userId.value).map { mapToDomain(it) }
    }

    override fun findAllBlockingUsers(
        blockedUserId: UserId
    ): List<BlockedUser> {
        return blockedUserRepository.findAllByBlockedUserId(blockedUserId.value).map { mapToDomain(it) }
    }

    override fun isUserBlocked(
        userId: UserId,
        blockedUserId: UserId
    ): Boolean {
        return blockedUserRepository.existsByUserIdAndBlockedUserId(userId.value, blockedUserId.value)
    }

    private fun mapToDomain(
        entity: BlockedUserEntity
    ): BlockedUser {
        return BlockedUser(
            id = entity.id?.let { BlockedUserId.from(it) },
            userId = UserId.Companion.from(entity.user.id),
            blockedUserId = UserId.Companion.from(entity.blockedUser.id),
            createdAt = entity.blockedAt
        )
    }

}