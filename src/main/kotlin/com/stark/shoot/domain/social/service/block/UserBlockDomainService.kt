package com.stark.shoot.domain.social.service.block

import com.stark.shoot.domain.social.BlockedUser
import com.stark.shoot.domain.user.vo.UserId

class UserBlockDomainService {

    /**
     * 사용자 차단 처리
     *
     * @param userId 차단을 수행하는 사용자 ID
     * @param targetId 차단할 사용자 ID
     * @return 생성된 차단 관계
     */
    fun block(
        userId: UserId,
        targetId: UserId
    ): BlockedUser {
        require(userId != targetId) { "자신을 차단할 수 없습니다." }
        return BlockedUser.create(userId, targetId)
    }

    /**
     * 사용자 차단 해제 처리
     *
     * @param userId 차단을 해제하는 사용자 ID
     * @param targetId 차단 해제할 사용자 ID
     */
    fun unblock(
        userId: UserId,
        targetId: UserId
    ) {
        // 차단 해제는 단순히 차단 관계를 삭제하는 것이므로 별도의 도메인 로직이 필요 없음
    }

}
