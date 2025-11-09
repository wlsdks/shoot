package com.stark.shoot.domain.social

import com.stark.shoot.domain.social.vo.BlockedUserId
import com.stark.shoot.domain.shared.UserId
import com.stark.shoot.infrastructure.annotation.AggregateRoot
import java.time.Instant

/**
 * 차단된 사용자 애그리게이트
 * 한 사용자가 다른 사용자를 차단한 관계를 나타냅니다.
 */
@AggregateRoot
data class BlockedUser(
    val id: BlockedUserId? = null,
    val userId: UserId,
    val blockedUserId: UserId,
    var createdAt: Instant = Instant.now(),
) {
    companion object {
        /**
         * 사용자 차단 생성
         *
         * @param userId 차단을 수행한 사용자 ID
         * @param blockedUserId 차단된 사용자 ID
         * @return 생성된 차단 관계
         */
        fun create(userId: UserId, blockedUserId: UserId): BlockedUser {
            return BlockedUser(
                userId = userId,
                blockedUserId = blockedUserId
            )
        }
    }
}