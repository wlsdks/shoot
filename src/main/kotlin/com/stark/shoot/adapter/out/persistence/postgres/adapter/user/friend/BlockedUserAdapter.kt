package com.stark.shoot.adapter.out.persistence.postgres.adapter.user.friend

import com.stark.shoot.adapter.out.persistence.postgres.entity.BlockedUserEntity
import com.stark.shoot.adapter.out.persistence.postgres.repository.BlockedUserRepository
import com.stark.shoot.adapter.out.persistence.postgres.repository.UserRepository
import com.stark.shoot.application.port.out.user.friend.BlockedUserCommandPort
import com.stark.shoot.application.port.out.user.friend.BlockedUserPort
import com.stark.shoot.application.port.out.user.friend.BlockedUserQueryPort
import com.stark.shoot.domain.user.BlockedUser
import com.stark.shoot.domain.user.vo.UserId
import com.stark.shoot.infrastructure.annotation.Adapter
import com.stark.shoot.infrastructure.exception.web.ResourceNotFoundException

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
        val user = userRepository.findById(blockedUser.userId.value)
            .orElseThrow { ResourceNotFoundException("사용자를 찾을 수 없습니다: ${blockedUser.userId.value}") }

        val blockedUserEntity = userRepository.findById(blockedUser.blockedUserId.value)
            .orElseThrow { ResourceNotFoundException("사용자를 찾을 수 없습니다: ${blockedUser.blockedUserId.value}") }

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
