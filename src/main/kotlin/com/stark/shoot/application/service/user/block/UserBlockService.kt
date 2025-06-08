package com.stark.shoot.application.service.user.block

import com.stark.shoot.application.port.`in`.user.block.UserBlockUseCase
import com.stark.shoot.application.port.out.user.FindUserPort
import com.stark.shoot.application.port.out.user.UserUpdatePort
import com.stark.shoot.domain.service.user.block.UserBlockDomainService
import com.stark.shoot.infrastructure.annotation.UseCase
import com.stark.shoot.infrastructure.exception.web.ResourceNotFoundException
import org.springframework.transaction.annotation.Transactional

@Transactional
@UseCase
class UserBlockService(
    private val findUserPort: FindUserPort,
    private val userUpdatePort: UserUpdatePort,
    private val userBlockDomainService: UserBlockDomainService,
) : UserBlockUseCase {

    override fun blockUser(currentUserId: Long, targetUserId: Long) {
        val current = findUserPort.findUserWithAllRelationshipsById(currentUserId)
            ?: throw ResourceNotFoundException("사용자를 찾을 수 없습니다: $currentUserId")
        if (!findUserPort.existsById(targetUserId)) {
            throw ResourceNotFoundException("사용자를 찾을 수 없습니다: $targetUserId")
        }
        val updated = userBlockDomainService.block(current, targetUserId)
        userUpdatePort.updateUser(updated)
    }

    override fun unblockUser(currentUserId: Long, targetUserId: Long) {
        val current = findUserPort.findUserWithAllRelationshipsById(currentUserId)
            ?: throw ResourceNotFoundException("사용자를 찾을 수 없습니다: $currentUserId")
        val updated = userBlockDomainService.unblock(current, targetUserId)
        userUpdatePort.updateUser(updated)
    }
}
