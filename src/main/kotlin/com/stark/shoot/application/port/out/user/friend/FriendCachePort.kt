package com.stark.shoot.application.port.out.user.friend

import com.stark.shoot.domain.common.vo.UserId

/**
 * 친구 관련 캐시를 관리하는 포트 인터페이스
 */
interface FriendCachePort {
    /**
     * 특정 사용자의 친구 추천 캐시를 무효화합니다.
     * 이 메서드는 친구 요청이 발생했을 때 호출되어 캐시를 갱신합니다.
     *
     * @param userId 캐시를 무효화할 사용자 ID
     */
    fun invalidateUserCache(userId: UserId)
}