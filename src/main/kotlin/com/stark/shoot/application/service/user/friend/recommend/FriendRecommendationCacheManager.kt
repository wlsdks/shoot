package com.stark.shoot.application.service.user.friend.recommend

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.stark.shoot.domain.user.User
import com.stark.shoot.domain.user.vo.UserId
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * 친구 추천 캐시 관리자
 *
 * Redis와 로컬 메모리를 활용한 2-tier 캐시를 관리합니다.
 * Redis 장애 시 로컬 캐시를 백업으로 사용하여 가용성을 보장합니다.
 */
@Component
class FriendRecommendationCacheManager(
    private val redisStringTemplate: StringRedisTemplate,
    private val objectMapper: ObjectMapper,
    @Value("\${app.friend-recommend.redis-cache-ttl-minutes:30}") private val redisCacheTtlMinutes: Long = 30,
    @Value("\${app.friend-recommend.local-cache-ttl-minutes:10}") private val localCacheTtlMinutes: Long = 10
) {
    private val logger = KotlinLogging.logger {}

    // 로컬 메모리 캐시 (Redis 장애 시 백업)
    private val localCache = ConcurrentHashMap<String, Pair<List<User>, Long>>()

    companion object {
        private const val CACHE_KEY_PREFIX = "friend_recommend"
    }

    /**
     * 캐시 키 생성
     *
     * @param userId 사용자 ID
     * @param limit 조회 제한 수
     * @return 캐시 키
     */
    fun getCacheKey(userId: UserId, limit: Int): String {
        return "$CACHE_KEY_PREFIX:${userId.value}:$limit"
    }

    /**
     * 캐시에서 추천 목록 조회 (Redis → 로컬 캐시 순서)
     *
     * @param cacheKey 캐시 키
     * @return 추천 친구 목록 또는 null
     */
    fun getFromCache(cacheKey: String): List<User>? {
        // Redis 캐시 조회
        val redisResult = getFromRedis(cacheKey)
        if (redisResult != null) {
            return redisResult
        }

        // 로컬 캐시 조회 (Redis 장애 시 백업)
        return getFromLocalCache(cacheKey)
    }

    /**
     * Redis에서 캐시 조회
     */
    private fun getFromRedis(cacheKey: String): List<User>? {
        return try {
            val json = redisStringTemplate.opsForValue().get(cacheKey)
            if (json != null) {
                objectMapper.readValue(json, object : TypeReference<List<User>>() {})
            } else null
        } catch (e: JsonProcessingException) {
            logger.warn(e) { "Redis 캐시 JSON 파싱 실패: $cacheKey" }
            null
        } catch (e: Exception) {
            logger.warn(e) { "Redis 캐시 조회 실패: $cacheKey" }
            null
        }
    }

    /**
     * 로컬 캐시에서 조회
     */
    private fun getFromLocalCache(cacheKey: String): List<User>? {
        val localCached = localCache[cacheKey] ?: return null
        val (users, timestamp) = localCached

        // 로컬 캐시 유효 시간 확인
        return if (isLocalCacheValid(timestamp)) {
            users
        } else {
            // 만료된 캐시 제거
            localCache.remove(cacheKey)
            null
        }
    }

    /**
     * 로컬 캐시 유효성 검사
     */
    private fun isLocalCacheValid(timestamp: Long): Boolean {
        return System.currentTimeMillis() - timestamp < TimeUnit.MINUTES.toMillis(localCacheTtlMinutes)
    }

    /**
     * 추천 목록을 캐시에 저장 (Redis + 로컬)
     *
     * @param cacheKey 캐시 키
     * @param users 추천 친구 목록
     */
    fun saveToCache(cacheKey: String, users: List<User>) {
        // Redis 캐시에 저장
        saveToRedis(cacheKey, users)

        // 로컬 캐시에도 저장 (백업)
        localCache[cacheKey] = Pair(users, System.currentTimeMillis())
    }

    /**
     * Redis에 캐시 저장
     */
    private fun saveToRedis(cacheKey: String, users: List<User>) {
        try {
            val jsonString = objectMapper.writeValueAsString(users)
            redisStringTemplate.opsForValue().set(cacheKey, jsonString, Duration.ofMinutes(redisCacheTtlMinutes))
        } catch (e: JsonProcessingException) {
            logger.warn(e) { "Redis 캐시 JSON 직렬화 실패: $cacheKey" }
        } catch (e: Exception) {
            logger.warn(e) { "Redis 캐시 저장 실패: $cacheKey" }
        }
    }

    /**
     * 특정 사용자의 추천 친구 캐시를 무효화합니다.
     *
     * @param userId 캐시를 무효화할 사용자 ID
     */
    fun invalidateUserCache(userId: UserId) {
        val cacheKeyPattern = "$CACHE_KEY_PREFIX:${userId.value}:"

        // 로컬 캐시 무효화
        val localKeysToRemove = localCache.keys.filter { it.startsWith(cacheKeyPattern) }
        localKeysToRemove.forEach { key -> localCache.remove(key) }

        if (localKeysToRemove.isNotEmpty()) {
            logger.debug { "사용자 로컬 캐시 무효화: userId=${userId.value}, 제거된 항목=${localKeysToRemove.size}개" }
        }

        // Redis 캐시 무효화
        invalidateRedisCache(cacheKeyPattern, userId)
    }

    /**
     * Redis 캐시 무효화
     */
    private fun invalidateRedisCache(pattern: String, userId: UserId) {
        try {
            val redisKeys = redisStringTemplate.keys("$pattern*")
            if (!redisKeys.isNullOrEmpty()) {
                redisStringTemplate.delete(redisKeys)
                logger.debug { "사용자 Redis 캐시 무효화: userId=${userId.value}, 제거된 항목=${redisKeys.size}개" }
            }
        } catch (e: Exception) {
            logger.warn(e) { "Redis 캐시 무효화 실패: userId=${userId.value}" }
        }
    }

    /**
     * 만료된 로컬 캐시 항목 정리 (스케줄링)
     * 설정된 간격(기본 15분)마다 실행
     */
    @Scheduled(
        fixedRateString = "\${app.friend-recommend.cache-cleanup-interval-minutes:15}",
        timeUnit = TimeUnit.MINUTES
    )
    fun cleanupExpiredCacheEntries() {
        val beforeSize = localCache.size
        val currentTime = System.currentTimeMillis()
        val expirationThreshold = TimeUnit.MINUTES.toMillis(localCacheTtlMinutes)

        // 만료된 항목 제거
        localCache.entries.removeIf { (_, value) ->
            currentTime - value.second > expirationThreshold
        }

        val removedCount = beforeSize - localCache.size
        if (removedCount > 0) {
            logger.info { "로컬 캐시 정리 완료: ${removedCount}개 항목 제거됨 (현재 캐시 크기: ${localCache.size})" }
        }
    }
}
