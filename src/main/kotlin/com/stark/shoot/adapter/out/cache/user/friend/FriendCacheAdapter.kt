package com.stark.shoot.adapter.out.cache.user.friend

import com.stark.shoot.application.port.out.user.friend.FriendCachePort
import com.stark.shoot.application.service.user.friend.recommend.RecommendFriendService
import com.stark.shoot.domain.user.vo.UserId
import com.stark.shoot.infrastructure.annotation.Adapter

/**
 * 친구 관련 캐시를 관리하는 어댑터 구현
 */
@Adapter
class FriendCacheAdapter(
    private val recommendFriendService: RecommendFriendService
) : FriendCachePort {

    /**
     * 특정 사용자의 친구 추천 캐시를 무효화합니다.
     * 이 메서드는 친구 요청이 발생했을 때 호출되어 캐시를 갱신합니다.
     *
     * @param userId 캐시를 무효화할 사용자 ID
     */
    override fun invalidateUserCache(userId: UserId) {
        recommendFriendService.invalidateUserCache(userId)
    }

}