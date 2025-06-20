package com.stark.shoot.application.port.out.cache

import com.stark.shoot.domain.common.vo.UserId

/**
 * 캐시 무효화를 위한 포트
 * 애플리케이션 서비스가 캐시 무효화 작업을 요청할 수 있는 인터페이스를 제공합니다.
 */
interface CacheInvalidationPort {
    /**
     * 사용자 관련 추천 캐시를 무효화합니다.
     *
     * @param userId 사용자 ID
     */
    fun invalidateRecommendationCache(userId: UserId)

    /**
     * 여러 사용자의 추천 캐시를 한 번에 무효화합니다.
     *
     * @param userIds 사용자 ID 목록
     */
    fun invalidateRecommendationCaches(userIds: Collection<UserId>)
}