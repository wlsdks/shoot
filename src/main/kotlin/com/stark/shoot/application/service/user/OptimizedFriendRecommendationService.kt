package com.stark.shoot.application.service.user

import com.stark.shoot.adapter.`in`.web.dto.user.FriendResponse
import com.stark.shoot.application.port.`in`.user.friend.RecommendFriendsUseCase
import com.stark.shoot.application.port.out.user.RecommendFriendPort
import com.stark.shoot.domain.chat.user.User
import io.github.oshai.kotlinlogging.KotlinLogging
import org.bson.types.ObjectId
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * 최적화된 친구 추천 서비스
 * - 캐싱 적용
 * - 주기적 사전 계산
 * - 리소스 사용량 제한
 */
@Service
class OptimizedFriendRecommendationService(
    private val recommendFriendPort: RecommendFriendPort,
    private val redisTemplate: RedisTemplate<String, List<User>>,
    private val redisStringTemplate: RedisTemplate<String, String>
) : RecommendFriendsUseCase {

    private val logger = KotlinLogging.logger {}

    // 메모리 캐시 (Redis 장애 시 백업)
    private val localCache = ConcurrentHashMap<String, Pair<List<User>, Long>>()

    // 현재 계산 중인 사용자 ID 목록 (중복 계산 방지)
    private val inProgressUsers = ConcurrentHashMap.newKeySet<String>()

    // 캐시 키 생성
    private fun getCacheKey(userId: ObjectId, maxDepth: Int): String {
        return "friend_recommend:${userId}:$maxDepth"
    }

    // 최대 추천 사용자 수 제한
    private val MAX_RECOMMENDATIONS = 100

    // 캐시 유효 시간 (분)
    private val CACHE_TTL_MINUTES = 120L

    /**
     * BFS 기반 친구 추천
     * - 캐싱 적용
     * - 페이징 지원
     */
    override fun findBFSRecommendedUsers(
        userId: ObjectId,
        maxDepth: Int,
        skip: Int,
        limit: Int
    ): List<FriendResponse> {
        val cacheKey = getCacheKey(userId, maxDepth)

        // 1. 캐시 확인
        val cachedUsers = getCachedRecommendations(cacheKey)

        // 2. 캐시 데이터가 있으면 페이징해서 반환
        if (cachedUsers != null) {
            logger.debug { "캐시에서 추천 친구 목록 조회: $userId (${cachedUsers.size}명)" }
            return paginateAndConvert(cachedUsers, skip, limit)
        }

        // 3. 중복 계산 방지
        val userIdStr = userId.toString()
        if (inProgressUsers.contains(userIdStr)) {
            logger.info { "이미 추천 목록 계산 중: $userId" }
            // 빈 목록 반환 (다음 요청에서 캐시된 결과를 받게 됨)
            return emptyList()
        }

        try {
            inProgressUsers.add(userIdStr)

            // 4. 데이터베이스에서 추천 친구 계산
            val recommendations = calculateAndCacheRecommendations(userId, maxDepth, cacheKey, limit)

            // 5. 페이징 및 변환
            return paginateAndConvert(recommendations, skip, limit)
        } finally {
            inProgressUsers.remove(userIdStr)
        }
    }

    /**
     * 캐시에서 추천 목록 조회
     */
    private fun getCachedRecommendations(cacheKey: String): List<User>? {
        // Redis 캐시 조회
        val redisResult = try {
            redisTemplate.opsForValue().get(cacheKey)
        } catch (e: Exception) {
            logger.warn(e) { "Redis 캐시 조회 실패: $cacheKey" }
            null
        }

        if (redisResult != null) {
            return redisResult
        }

        // 로컬 캐시 조회 (Redis 장애 시 백업)
        val localCached = localCache[cacheKey]
        if (localCached != null) {
            val (users, timestamp) = localCached
            // 로컬 캐시 유효 시간 (30분)
            if (System.currentTimeMillis() - timestamp < TimeUnit.MINUTES.toMillis(30)) {
                return users
            } else {
                // 만료된 캐시 제거
                localCache.remove(cacheKey)
            }
        }

        return null
    }

    /**
     * 추천 목록 계산 및 캐싱
     */
    private fun calculateAndCacheRecommendations(
        userId: ObjectId,
        maxDepth: Int,
        cacheKey: String,
        limit: Int
    ): List<User> {
        logger.info { "친구 추천 목록 계산 시작: $userId (깊이: $maxDepth)" }

        val recommendedUsers = recommendFriendPort.findBFSRecommendedUsers(
            userId,
            maxDepth,
            0,
            limit
        )

        logger.info { "친구 추천 목록 계산 완료: $userId (${recommendedUsers.size}명)" }

        // Redis 캐시에 저장
        try {
            redisTemplate.opsForValue().set(cacheKey, recommendedUsers, Duration.ofMinutes(CACHE_TTL_MINUTES))
        } catch (e: Exception) {
            logger.warn(e) { "Redis 캐시 저장 실패: $cacheKey" }
        }

        // 로컬 캐시에도 저장 (백업)
        localCache[cacheKey] = Pair(recommendedUsers, System.currentTimeMillis())

        return recommendedUsers
    }

    /**
     * 페이징 및 응답 DTO 변환
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
                    id = user.id.toString(),
                    username = user.username
                )
            }
    }

    /**
     * 인기 사용자(팔로워가 많은)에 대한 추천 목록 주기적 사전 계산
     * - 매일 오전 4시에 실행
     */
    @Scheduled(cron = "0 0 4 * * ?")
    fun precalculatePopularUserRecommendations() {
        try {
            logger.info { "인기 사용자 추천 목록 사전 계산 시작" }

            // 사전 계산 필요 여부 확인 (동시 실행 방지)
            val lockKey = "friend_recommend:precalculation:lock"
            val lockSuccess = redisStringTemplate.opsForValue()
                .setIfAbsent(lockKey, "locked", Duration.ofHours(2))

            if (lockSuccess != true) {
                logger.info { "이미 다른 서버에서 사전 계산 중입니다." }
                return
            }

            // 인기 사용자(친구가 많은) 목록 조회 (이 부분은 필요에 따라 구현)
            // val popularUsers = userRepository.findPopularUsers(50)

            // 예시: 고정 사용자 ID 목록
            val popularUserIds = listOf(
                "60f1a7b8c9d0e1f2a3b4c5d6",
                "60f1a7b8c9d0e1f2a3b4c5d7",
                "60f1a7b8c9d0e1f2a3b4c5d8"
            )

            // 각 인기 사용자에 대해 추천 목록 사전 계산
            popularUserIds.forEach { userIdStr ->
                try {
                    val userId = ObjectId(userIdStr)
                    val cacheKey = getCacheKey(userId, 2) // 깊이 2로 고정

                    if (!redisTemplate.hasKey(cacheKey)) {
                        calculateAndCacheRecommendations(userId, 2, cacheKey, MAX_RECOMMENDATIONS)
                        // 과부하 방지를 위한 짧은 지연
                        Thread.sleep(100)
                    }
                } catch (e: Exception) {
                    logger.error(e) { "사용자 추천 목록 사전 계산 실패: $userIdStr" }
                }
            }

            logger.info { "인기 사용자 추천 목록 사전 계산 완료" }
        } catch (e: Exception) {
            logger.error(e) { "인기 사용자 추천 목록 사전 계산 중 오류 발생" }
        } finally {
            // 락 해제
            redisStringTemplate.delete("friend_recommend:precalculation:lock")
        }
    }

    /**
     * 캐시 정리 (만료된 데이터)
     * - 매시간 실행
     */
    @Scheduled(cron = "0 0 * * * ?")
    fun cleanupExpiredCache() {
        try {
            // 로컬 캐시 정리
            val now = System.currentTimeMillis()
            val expiredKeys = localCache.entries
                .filter { (_, value) -> now - value.second > TimeUnit.MINUTES.toMillis(30) }
                .map { it.key }
                .toList()

            expiredKeys.forEach { localCache.remove(it) }

            logger.debug { "만료된 캐시 항목 제거: ${expiredKeys.size}개" }
        } catch (e: Exception) {
            logger.warn(e) { "캐시 정리 중 오류 발생" }
        }
    }

}