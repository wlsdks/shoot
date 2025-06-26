package com.stark.shoot.adapter.persistence.cache

import com.stark.shoot.adapter.out.cache.RedisCacheInvalidationAdapter
import com.stark.shoot.application.port.out.user.friend.FriendCachePort
import com.stark.shoot.domain.user.vo.UserId
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import org.springframework.data.redis.core.StringRedisTemplate

@DisplayName("Redis 캐시 무효화 어댑터 테스트")
class RedisCacheInvalidationAdapterTest {

    private val redisTemplate = mock(StringRedisTemplate::class.java)
    private val friendCachePort = mock(FriendCachePort::class.java)
    private val adapter = RedisCacheInvalidationAdapter(redisTemplate, friendCachePort)

    @Test
    @DisplayName("[happy] 단일 사용자의 추천 캐시를 무효화할 수 있다")
    fun `단일 사용자의 추천 캐시를 무효화할 수 있다`() {
        // given
        val userId = UserId.from(1L)
        val cacheKeyPattern = "friend_recommend:${userId.value}:*"
        val keys = setOf("friend_recommend:1:123", "friend_recommend:1:456")
        
        `when`(redisTemplate.keys(cacheKeyPattern)).thenReturn(keys)
        
        // when
        adapter.invalidateRecommendationCache(userId)
        
        // then
        verify(redisTemplate).keys(cacheKeyPattern)
        verify(redisTemplate).delete(keys)
        verify(friendCachePort).invalidateUserCache(userId)
    }
    
    @Test
    @DisplayName("[happy] 캐시 키가 없어도 예외가 발생하지 않는다")
    fun `캐시 키가 없어도 예외가 발생하지 않는다`() {
        // given
        val userId = UserId.from(1L)
        val cacheKeyPattern = "friend_recommend:${userId.value}:*"
        
        `when`(redisTemplate.keys(cacheKeyPattern)).thenReturn(emptySet())
        
        // when
        adapter.invalidateRecommendationCache(userId)
        
        // then
        verify(redisTemplate).keys(cacheKeyPattern)
        verify(redisTemplate, never()).delete(anySet<String>())
        verify(friendCachePort).invalidateUserCache(userId)
    }
    
    @Test
    @DisplayName("[happy] Redis 예외가 발생해도 처리를 계속한다")
    fun `Redis 예외가 발생해도 처리를 계속한다`() {
        // given
        val userId = UserId.from(1L)
        val cacheKeyPattern = "friend_recommend:${userId.value}:*"
        
        `when`(redisTemplate.keys(cacheKeyPattern)).thenThrow(RuntimeException("Redis connection error"))
        
        // when
        adapter.invalidateRecommendationCache(userId)
        
        // then
        verify(redisTemplate).keys(cacheKeyPattern)
        verify(redisTemplate, never()).delete(anySet<String>())
        // 예외가 발생해도 friendCachePort는 호출되지 않음
        verify(friendCachePort, never()).invalidateUserCache(userId)
    }
    
    @Test
    @DisplayName("[happy] 여러 사용자의 추천 캐시를 무효화할 수 있다")
    fun `여러 사용자의 추천 캐시를 무효화할 수 있다`() {
        // given
        val userIds = listOf(
            UserId.from(1L),
            UserId.from(2L),
            UserId.from(3L)
        )
        
        // 각 사용자 ID에 대한 캐시 키 패턴 및 키 설정
        for (userId in userIds) {
            val cacheKeyPattern = "friend_recommend:${userId.value}:*"
            val keys = setOf("friend_recommend:${userId.value}:123")
            `when`(redisTemplate.keys(cacheKeyPattern)).thenReturn(keys)
        }
        
        // when
        adapter.invalidateRecommendationCaches(userIds)
        
        // then
        for (userId in userIds) {
            val cacheKeyPattern = "friend_recommend:${userId.value}:*"
            verify(redisTemplate).keys(cacheKeyPattern)
            verify(friendCachePort).invalidateUserCache(userId)
        }
    }
}