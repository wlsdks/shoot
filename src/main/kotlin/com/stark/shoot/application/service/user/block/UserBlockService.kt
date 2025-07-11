package com.stark.shoot.application.service.user.block

import com.stark.shoot.application.port.`in`.user.block.UserBlockUseCase
import com.stark.shoot.application.port.`in`.user.block.command.BlockUserCommand
import com.stark.shoot.application.port.`in`.user.block.command.UnblockUserCommand
import com.stark.shoot.application.port.out.user.UserQueryPort
import com.stark.shoot.application.port.out.user.block.BlockedUserCommandPort
import com.stark.shoot.application.port.out.user.block.BlockedUserQueryPort
import com.stark.shoot.domain.user.service.block.UserBlockDomainService
import com.stark.shoot.infrastructure.annotation.UseCase
import com.stark.shoot.infrastructure.exception.web.ResourceNotFoundException
import org.springframework.transaction.annotation.Transactional

@Transactional
@UseCase
class UserBlockService(
    private val userQueryPort: UserQueryPort,
    private val blockedUserCommandPort: BlockedUserCommandPort,
    private val blockedUserQueryPort: BlockedUserQueryPort,
    private val userBlockDomainService: UserBlockDomainService,
) : UserBlockUseCase {

    override fun blockUser(command: BlockUserCommand) {
        val currentUserId = command.currentUserId
        val targetUserId = command.targetUserId

        // 사용자 존재 여부 확인
        if (!userQueryPort.existsById(currentUserId)) {
            throw ResourceNotFoundException("사용자를 찾을 수 없습니다: $currentUserId")
        }

        if (!userQueryPort.existsById(targetUserId)) {
            throw ResourceNotFoundException("사용자를 찾을 수 없습니다: $targetUserId")
        }

        // 이미 차단 관계가 존재하는지 확인
        if (blockedUserQueryPort.isUserBlocked(currentUserId, targetUserId)) {
            // 이미 차단된 경우 아무 작업도 수행하지 않음
            return
        }

        // 도메인 서비스를 사용하여 차단 관계 생성
        val blockedUser = userBlockDomainService.block(currentUserId, targetUserId)

        // 차단 관계 저장
        blockedUserCommandPort.blockUser(blockedUser)
    }

    override fun unblockUser(command: UnblockUserCommand) {
        val currentUserId = command.currentUserId
        val targetUserId = command.targetUserId

        // 사용자 존재 여부 확인
        if (!userQueryPort.existsById(currentUserId)) {
            throw ResourceNotFoundException("사용자를 찾을 수 없습니다: $currentUserId")
        }

        if (!userQueryPort.existsById(targetUserId)) {
            throw ResourceNotFoundException("사용자를 찾을 수 없습니다: $targetUserId")
        }

        // 도메인 서비스를 사용하여 차단 해제 처리
        userBlockDomainService.unblock(currentUserId, targetUserId)

        // 차단 관계 삭제
        blockedUserCommandPort.unblockUser(currentUserId, targetUserId)
    }

}
