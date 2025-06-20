package com.stark.shoot.adapter.out.cache

import com.stark.shoot.application.port.out.cache.CacheInvalidationPort
import com.stark.shoot.application.port.out.user.friend.FriendCachePort
import com.stark.shoot.domain.common.vo.UserId
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component
import io.github.oshai.kotlinlogging.KotlinLogging

/**
 * Redis를 사용한 캐시 무효화 어댑터
 * CacheInvalidationPort 인터페이스를 구현하여 Redis 관련 코드를 캡슐화합니다.
 */
@Component
class RedisCacheInvalidationAdapter(
    private val redisTemplate: StringRedisTemplate,
    private val friendCachePort: FriendCachePort
) : CacheInvalidationPort {

    private val logger = KotlinLogging.logger {}

    /**
     * 사용자 관련 추천 캐시를 무효화합니다.
     *
     * @param userId 사용자 ID
     */
    override fun invalidateRecommendationCache(userId: Long) {
        try {
            // 추천 친구 캐시 키 패턴
            val cacheKeyPattern = "friend_recommend:$userId:*"

            // 해당 패턴의 모든 키 조회
            val keys = redisTemplate.keys(cacheKeyPattern)

            // 키가 있으면 삭제
            if (!keys.isNullOrEmpty()) {
                redisTemplate.delete(keys)
                logger.debug { "캐시 무효화 완료: userId=$userId, 삭제된 키 개수=${keys.size}" }
            }

            // 친구 추천 캐시 무효화
            friendCachePort.invalidateUserCache(UserId.from(userId))
        } catch (e: Exception) {
            // 캐시 삭제 실패는 치명적인 오류가 아니므로 로깅만 하고 계속 진행
            logger.warn(e) { "캐시 삭제 실패: userId=$userId" }
        }
    }

    /**
     * 여러 사용자의 추천 캐시를 한 번에 무효화합니다.
     *
     * @param userIds 사용자 ID 목록
     */
    override fun invalidateRecommendationCaches(userIds: Collection<Long>) {
        userIds.forEach { userId ->
            invalidateRecommendationCache(userId)
        }
    }
}
