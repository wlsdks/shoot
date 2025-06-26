package com.stark.shoot.adapter.persistence.cache.user.friend

import com.stark.shoot.adapter.out.cache.user.friend.FriendCacheAdapter
import com.stark.shoot.application.service.user.friend.recommend.RecommendFriendService
import com.stark.shoot.domain.user.vo.UserId
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify

@DisplayName("친구 캐시 어댑터 테스트")
class FriendCacheAdapterTest {

    private val recommendFriendService = mock(RecommendFriendService::class.java)
    private val adapter = FriendCacheAdapter(recommendFriendService)

    @Test
    @DisplayName("[happy] 사용자의 친구 추천 캐시를 무효화할 수 있다")
    fun `사용자의 친구 추천 캐시를 무효화할 수 있다`() {
        // given
        val userId = UserId.from(1L)
        
        // when
        adapter.invalidateUserCache(userId)
        
        // then
        verify(recommendFriendService).invalidateUserCache(userId)
    }
    
    @Test
    @DisplayName("[happy] 여러 사용자의 친구 추천 캐시를 무효화할 수 있다")
    fun `여러 사용자의 친구 추천 캐시를 무효화할 수 있다`() {
        // given
        val userIds = listOf(
            UserId.from(1L),
            UserId.from(2L),
            UserId.from(3L)
        )
        
        // when
        userIds.forEach { userId ->
            adapter.invalidateUserCache(userId)
        }
        
        // then
        userIds.forEach { userId ->
            verify(recommendFriendService).invalidateUserCache(userId)
        }
    }
}