package com.stark.shoot.adapter.out.persistence.postgres.adapter.user.friend

import com.stark.shoot.adapter.out.persistence.postgres.entity.BlockedUserEntity
import com.stark.shoot.adapter.out.persistence.postgres.repository.BlockedUserRepository
import com.stark.shoot.adapter.out.persistence.postgres.repository.UserRepository
import com.stark.shoot.application.port.out.user.friend.BlockedUserPort
import com.stark.shoot.domain.user.BlockedUser
import com.stark.shoot.domain.user.vo.UserId
import com.stark.shoot.infrastructure.annotation.Adapter

@Adapter
class BlockedUserAdapter(
    private val blockedUserRepository: BlockedUserRepository,
    private val userRepository: UserRepository
) : BlockedUserPort {

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
            userId = UserId.from(entity.user.id),
            blockedUserId = UserId.from(entity.blockedUser.id),
            createdAt = entity.blockedAt
        )
    }

}
