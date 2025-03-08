package com.stark.shoot.application.service.user.friend.recommend

import com.fasterxml.jackson.databind.ObjectMapper
import com.stark.shoot.application.port.out.user.friend.RecommendFriendPort
import com.stark.shoot.domain.chat.user.User
import com.stark.shoot.infrastructure.annotation.UseCase
import io.github.oshai.kotlinlogging.KotlinLogging
import org.bson.types.ObjectId
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.scheduling.annotation.Scheduled
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

@UseCase
class FriendRecommendScheduleService(
    private val recommendFriendPort: RecommendFriendPort,
    private val redisStringTemplate: RedisTemplate<String, String>,
    private val objectMapper: ObjectMapper
) {

    private val logger = KotlinLogging.logger {}

    // 메모리 캐시 (Redis 장애 시 백업)
    private val localCache = ConcurrentHashMap<String, Pair<List<User>, Long>>()

    // 캐시 키 생성
    private fun getCacheKey(userId: ObjectId, maxDepth: Int): String {
        return "friend_recommend:${userId}:$maxDepth"
    }

    // 최대 추천 사용자 수 제한
    private val MAX_RECOMMENDATIONS = 100

    // 캐시 유효 시간 (분)
    private val CACHE_TTL_MINUTES = 120L

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

                    if (!redisStringTemplate.hasKey(cacheKey)) {
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

        // Redis 캐시에 JSON 문자열로 저장
        try {
            val jsonString = objectMapper.writeValueAsString(recommendedUsers)
            redisStringTemplate.opsForValue().set(cacheKey, jsonString, Duration.ofMinutes(CACHE_TTL_MINUTES))
        } catch (e: Exception) {
            logger.warn(e) { "Redis 캐시 저장 실패: $cacheKey" }
        }

        // 로컬 캐시에도 저장 (백업)
        localCache[cacheKey] = Pair(recommendedUsers, System.currentTimeMillis())

        return recommendedUsers
    }

}