package com.stark.shoot.application.service.user.block

import com.stark.shoot.application.port.`in`.user.block.UserBlockUseCase
import com.stark.shoot.application.port.out.user.FindUserPort
import com.stark.shoot.application.port.out.user.friend.BlockedUserPort
import com.stark.shoot.domain.user.service.block.UserBlockDomainService
import com.stark.shoot.domain.user.vo.UserId
import com.stark.shoot.infrastructure.annotation.UseCase
import com.stark.shoot.infrastructure.exception.web.ResourceNotFoundException
import org.springframework.transaction.annotation.Transactional

@Transactional
@UseCase
class UserBlockService(
    private val findUserPort: FindUserPort,
    private val blockedUserPort: BlockedUserPort,
    private val userBlockDomainService: UserBlockDomainService,
) : UserBlockUseCase {

    override fun blockUser(
        currentUserId: UserId,
        targetUserId: UserId
    ) {
        // 사용자 존재 여부 확인
        if (!findUserPort.existsById(currentUserId)) {
            throw ResourceNotFoundException("사용자를 찾을 수 없습니다: $currentUserId")
        }

        if (!findUserPort.existsById(targetUserId)) {
            throw ResourceNotFoundException("사용자를 찾을 수 없습니다: $targetUserId")
        }

        // 도메인 서비스를 사용하여 차단 관계 생성
        val blockedUser = userBlockDomainService.block(currentUserId, targetUserId)

        // 차단 관계 저장
        blockedUserPort.blockUser(blockedUser)
    }

    override fun unblockUser(
        currentUserId: UserId,
        targetUserId: UserId
    ) {
        // 사용자 존재 여부 확인
        if (!findUserPort.existsById(currentUserId)) {
            throw ResourceNotFoundException("사용자를 찾을 수 없습니다: $currentUserId")
        }

        if (!findUserPort.existsById(targetUserId)) {
            throw ResourceNotFoundException("사용자를 찾을 수 없습니다: $targetUserId")
        }

        // 도메인 서비스를 사용하여 차단 해제 처리
        userBlockDomainService.unblock(currentUserId, targetUserId)

        // 차단 관계 삭제
        blockedUserPort.unblockUser(currentUserId, targetUserId)
    }

}
