package com.stark.shoot.application.port.out.user.friend

import com.stark.shoot.domain.user.BlockedUser
import com.stark.shoot.domain.user.vo.UserId

/**
 * 차단된 사용자 관련 포트
 */
interface BlockedUserPort {
    /**
     * 사용자가 차단한 모든 사용자 조회
     *
     * @param userId 사용자 ID
     * @return 차단된 사용자 목록
     */
    fun findAllBlockedUsers(userId: UserId): List<BlockedUser>

    /**
     * 사용자를 차단한 모든 사용자 조회
     *
     * @param blockedUserId 차단된 사용자 ID
     * @return 차단한 사용자 목록
     */
    fun findAllBlockingUsers(blockedUserId: UserId): List<BlockedUser>

    /**
     * 사용자 차단 여부 확인
     *
     * @param userId 사용자 ID
     * @param blockedUserId 차단된 사용자 ID
     * @return 차단 여부
     */
    fun isUserBlocked(userId: UserId, blockedUserId: UserId): Boolean

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