package com.stark.shoot.application.service.user.friend.recommend

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.stark.shoot.adapter.`in`.web.dto.user.FriendResponse
import com.stark.shoot.application.port.`in`.user.friend.RecommendFriendsUseCase
import com.stark.shoot.application.port.out.user.FindUserPort
import com.stark.shoot.application.port.out.user.friend.RecommendFriendPort
import com.stark.shoot.domain.chat.user.User
import com.stark.shoot.infrastructure.annotation.UseCase
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.scheduling.annotation.Scheduled
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

@UseCase
class RecommendFriendService(
    private val recommendFriendPort: RecommendFriendPort,
    private val findUserPort: FindUserPort,
    private val redisStringTemplate: StringRedisTemplate,
    private val objectMapper: ObjectMapper,
    @Value("\${app.friend-recommend.redis-cache-ttl-minutes:30}") private val redisCacheTtlMinutes: Long = 30,
    @Value("\${app.friend-recommend.local-cache-ttl-minutes:10}") private val localCacheTtlMinutes: Long = 10,
    @Value("\${app.friend-recommend.cache-cleanup-interval-minutes:5}") private val cacheCleanupIntervalMinutes: Long = 5
) : RecommendFriendsUseCase {

    private val logger = KotlinLogging.logger {}

    // 메모리 캐시 (Redis 장애 시 백업)
    private val localCache = ConcurrentHashMap<String, Pair<List<User>, Long>>()

    // 현재 계산 중인 사용자 ID 목록 (중복 계산 방지)
    private val inProgressUsers = ConcurrentHashMap.newKeySet<String>()

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
    private fun getCacheKey(userId: Long, limit: Int): String {
        return "$CACHE_KEY_PREFIX:$userId:$limit"
    }

    /**
     * 친구 추천 구현
     * - 캐싱 적용
     * - 페이징 지원
     * 
     * @param userId 사용자 ID
     * @param skip 건너뛸 항목 수
     * @param limit 조회할 항목 수
     * @return 추천 친구 목록
     */
    override fun getRecommendedFriends(
        userId: Long,
        skip: Int,
        limit: Int
    ): List<FriendResponse> {
        // 캐시 키 생성
        val cacheKey = getCacheKey(userId, limit)

        // 캐시 확인
        val cachedUsers = getCachedRecommendations(cacheKey)

        // 캐시 데이터가 있으면 추가 필터링 후 페이징해서 반환
        if (cachedUsers != null) {
            logger.debug { "캐시에서 추천 친구 목록 조회: userId=$userId, 필터링 전=${cachedUsers.size}명" }

            // 캐시된 결과에서 이미 친구인 사용자와 친구 요청을 보낸 사용자 제외
            val filteredUsers = filterExistingRelationships(userId, cachedUsers)

            logger.debug { "캐시 결과 필터링 완료: userId=$userId, 필터링 후=${filteredUsers.size}명" }
            return paginateAndConvert(filteredUsers, skip, limit)
        }

        // 중복 계산 방지
        val userIdStr = userId.toString()
        if (inProgressUsers.contains(userIdStr)) {
            logger.info { "이미 추천 목록 계산 중: userId=$userId, 랜덤 유저 반환" }
            // 빈 목록 대신 랜덤 유저 반환
            val randomRecommendations = recommendFriendPort.recommendFriends(-1, limit * 2)

            // 랜덤 결과에서도 이미 친구인 사용자와 친구 요청을 보낸 사용자 제외
            val filteredRandomUsers = filterExistingRelationships(userId, randomRecommendations)

            return paginateAndConvert(filteredRandomUsers, skip, limit)
        }

        try {
            inProgressUsers.add(userIdStr)

            // 데이터베이스에서 추천 친구 계산
            val recommendations = calculateAndCacheRecommendations(userId, cacheKey, limit)

            // 이미 친구인 사용자와 친구 요청을 보낸 사용자 제외
            val filteredRecommendations = filterExistingRelationships(userId, recommendations)

            logger.debug { "새로 계산된 결과 필터링 완료: userId=$userId, 필터링 전=${recommendations.size}명, 필터링 후=${filteredRecommendations.size}명" }

            // 페이징 및 변환
            return paginateAndConvert(filteredRecommendations, skip, limit)
        } finally {
            inProgressUsers.remove(userIdStr)
        }
    }

    /**
     * 캐시에서 추천 목록 조회
     * 
     * @param cacheKey 캐시 키
     * @return 추천 친구 목록 또는 null
     */
    private fun getCachedRecommendations(cacheKey: String): List<User>? {
        // Redis 캐시 조회
        val redisResult = try {
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

        // Redis 캐시가 있으면 반환
        if (redisResult != null) {
            return redisResult
        }

        // 로컬 캐시 조회 (Redis 장애 시 백업)
        val localCached = localCache[cacheKey]
        if (localCached != null) {
            val (users, timestamp) = localCached
            // 로컬 캐시 유효 시간 확인
            if (isLocalCacheValid(timestamp)) {
                return users
            } else {
                // 만료된 캐시 제거
                localCache.remove(cacheKey)
            }
        }

        return null
    }

    /**
     * 로컬 캐시 유효성 검사
     * 
     * @param timestamp 캐시 생성 시간
     * @return 유효 여부
     */
    private fun isLocalCacheValid(timestamp: Long): Boolean {
        return System.currentTimeMillis() - timestamp < TimeUnit.MINUTES.toMillis(localCacheTtlMinutes)
    }

    /**
     * 추천 목록 계산 및 캐싱
     * 
     * @param userId 사용자 ID
     * @param cacheKey 캐시 키
     * @param limit 조회 제한 수
     * @return 추천 친구 목록
     */
    private fun calculateAndCacheRecommendations(
        userId: Long,
        cacheKey: String,
        limit: Int
    ): List<User> {
        var recommendedUsers = recommendFriendPort.recommendFriends(userId, limit * 2)
            .filter { it.id != userId } // 본인 제외

        // 추천 결과가 없으면 랜덤 유저로 대체
        if (recommendedUsers.isEmpty()) {
            logger.info { "추천 친구가 없음: userId=$userId, 랜덤 유저 반환" }
            recommendedUsers = recommendFriendPort.recommendFriends(-1, limit * 2)
                .filter { it.id != userId } // 본인 제외
        }

        // 로그 추가: 추천 결과 수 기록
        logger.debug { "추천 친구 계산 결과: userId=$userId, 결과=${recommendedUsers.size}명" }

        // Redis 캐시에 JSON 문자열로 저장
        try {
            val jsonString = objectMapper.writeValueAsString(recommendedUsers)
            redisStringTemplate.opsForValue().set(cacheKey, jsonString, Duration.ofMinutes(redisCacheTtlMinutes))
        } catch (e: JsonProcessingException) {
            logger.warn(e) { "Redis 캐시 JSON 직렬화 실패: $cacheKey" }
        } catch (e: Exception) {
            logger.warn(e) { "Redis 캐시 저장 실패: $cacheKey" }
        }

        // 로컬 캐시에도 저장 (백업)
        localCache[cacheKey] = Pair(recommendedUsers, System.currentTimeMillis())

        return recommendedUsers
    }

    /**
     * 페이징 및 응답 DTO 변환
     * 
     * @param users 사용자 목록
     * @param skip 건너뛸 항목 수
     * @param limit 조회할 항목 수
     * @return 변환된 친구 응답 목록
     */
    private fun paginateAndConvert(
        users: List<User>,
        skip: Int,
        limit: Int
    ): List<FriendResponse> {
        return users
            .drop(skip)
            .take(limit)
            .map { user ->
                FriendResponse(
                    id = user.id ?: 0L,
                    username = user.username.value,
                    nickname = user.nickname,
                    profileImageUrl = user.profileImageUrl
                )
            }
    }

    /**
     * 이미 친구인 사용자와 친구 요청을 보낸 사용자를 필터링합니다.
     * 
     * @param userId 현재 사용자 ID
     * @param users 필터링할 사용자 목록
     * @return 필터링된 사용자 목록
     */
    private fun filterExistingRelationships(userId: Long, users: List<User>): List<User> {
        // 이미 친구인 사용자 ID 목록
        val friendIds = mutableSetOf<Long>()

        // 보낸 친구 요청 ID 목록
        val outgoingRequestIds = mutableSetOf<Long>()

        // 받은 친구 요청 ID 목록
        val incomingRequestIds = mutableSetOf<Long>()

        try {
            // 친구 관계 확인
            users.forEach { user ->
                user.id?.let { targetId ->
                    if (findUserPort.checkFriendship(userId, targetId)) {
                        friendIds.add(targetId)
                    }

                    if (findUserPort.checkOutgoingFriendRequest(userId, targetId)) {
                        outgoingRequestIds.add(targetId)
                    }

                    if (findUserPort.checkIncomingFriendRequest(userId, targetId)) {
                        incomingRequestIds.add(targetId)
                    }
                }
            }

            logger.debug { "필터링 정보: userId=$userId, 친구=${friendIds.size}명, 보낸요청=${outgoingRequestIds.size}명, 받은요청=${incomingRequestIds.size}명" }
        } catch (e: Exception) {
            logger.warn(e) { "친구 관계 확인 중 오류 발생: userId=$userId" }
            // 오류 발생 시 원본 목록 반환
            return users
        }

        // 필터링된 목록 반환
        return users.filter { user ->
            user.id?.let { id ->
                !friendIds.contains(id) && !outgoingRequestIds.contains(id) && !incomingRequestIds.contains(id)
            } ?: true
        }
    }

    /**
     * 만료된 로컬 캐시 항목 정리
     * 설정된 간격(기본 15분)마다 실행되며, 설정된 시간(기본 30분) 이상 사용되지 않은 캐시 항목을 삭제합니다.
     */
    @Scheduled(fixedRateString = "\${app.friend-recommend.cache-cleanup-interval-minutes:15}", timeUnit = TimeUnit.MINUTES)
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

    /**
     * 특정 사용자의 추천 친구 캐시를 무효화합니다.
     * 이 메서드는 친구 요청이 발생했을 때 호출되어 캐시를 갱신합니다.
     *
     * @param userId 캐시를 무효화할 사용자 ID
     */
    fun invalidateUserCache(userId: Long) {
        val cacheKeyPattern = "$CACHE_KEY_PREFIX:$userId:"

        // 로컬 캐시 무효화
        val keysToRemove = localCache.keys.filter { it.startsWith(cacheKeyPattern) }
        keysToRemove.forEach { key ->
            localCache.remove(key)
        }

        if (keysToRemove.isNotEmpty()) {
            logger.debug { "사용자 로컬 캐시 무효화: userId=$userId, 제거된 항목=${keysToRemove.size}개" }
        }

        // Redis 캐시도 무효화
        try {
            val redisKeys = redisStringTemplate.keys(cacheKeyPattern)
            if (!redisKeys.isNullOrEmpty()) {
                redisStringTemplate.delete(redisKeys)
                logger.debug { "사용자 Redis 캐시 무효화: userId=$userId, 제거된 항목=${redisKeys.size}개" }
            }
        } catch (e: Exception) {
            logger.warn(e) { "Redis 캐시 무효화 실패: userId=$userId" }
        }
    }
}
