package com.stark.shoot.application.service.user.friend

import com.stark.shoot.application.port.out.cache.CacheInvalidationPort
import com.stark.shoot.domain.common.vo.UserId
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component

/**
 * 친구 관련 캐시 관리 컴포넌트
 * 친구 관련 서비스에서 중복되는 캐시 무효화 로직을 통합합니다.
 */
@Component
class FriendCacheManager(
    private val cacheInvalidationPort: CacheInvalidationPort
) {
    private val logger = KotlinLogging.logger {}

    /**
     * 친구 관계 변경 시 관련된 모든 사용자의 캐시를 무효화합니다.
     *
     * @param userIds 캐시를 무효화할 사용자 ID 목록
     */
    fun invalidateFriendCaches(userIds: List<UserId>) {
        if (userIds.isEmpty()) {
            return
        }

        logger.debug { "친구 관계 캐시 무효화: userIds=${userIds.joinToString()}" }
        cacheInvalidationPort.invalidateRecommendationCaches(userIds)
    }

    /**
     * 두 사용자 간의 친구 관계 변경 시 캐시를 무효화합니다.
     *
     * @param userId 첫 번째 사용자 ID
     * @param friendId 두 번째 사용자 ID
     */
    fun invalidateFriendshipCaches(userId: UserId, friendId: UserId) {
        invalidateFriendCaches(listOf(userId, friendId))
    }
}