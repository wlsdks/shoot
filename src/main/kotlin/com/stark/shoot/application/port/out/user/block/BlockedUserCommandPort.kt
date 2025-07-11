package com.stark.shoot.application.port.out.user.block

import com.stark.shoot.domain.user.BlockedUser
import com.stark.shoot.domain.user.vo.UserId

/**
 * 차단된 사용자 생성 및 수정 관련 포트
 */
interface BlockedUserCommandPort {
    /**
     * 사용자 차단
     *
     * @param blockedUser 차단 관계
     * @return 저장된 차단 관계
     */
    fun blockUser(blockedUser: BlockedUser): BlockedUser

    /**
     * 사용자 차단 해제
     *
     * @param userId 사용자 ID
     * @param blockedUserId 차단된 사용자 ID
     */
    fun unblockUser(userId: UserId, blockedUserId: UserId)
}