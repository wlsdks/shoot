package com.stark.shoot.domain.user

import com.stark.shoot.domain.user.vo.FriendshipId
import com.stark.shoot.domain.user.vo.UserId
import java.time.Instant

/**
 * 친구 관계 애그리게이트
 * 두 사용자 간의 친구 관계를 나타냅니다.
 */
data class Friendship(
    val id: FriendshipId? = null,
    val userId: UserId,
    val friendId: UserId,
    val createdAt: Instant = Instant.now(),
) {
    companion object {
        /**
         * 친구 관계 생성
         *
         * @param userId 사용자 ID
         * @param friendId 친구 ID
         * @return 생성된 친구 관계
         */
        fun create(userId: UserId, friendId: UserId): Friendship {
            return Friendship(
                userId = userId,
                friendId = friendId
            )
        }
    }
}
