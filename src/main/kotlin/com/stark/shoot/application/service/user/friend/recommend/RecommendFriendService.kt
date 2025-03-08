package com.stark.shoot.application.service.user.friend.recommend

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.stark.shoot.adapter.`in`.web.dto.user.FriendResponse
import com.stark.shoot.application.port.`in`.user.friend.RecommendFriendsUseCase
import com.stark.shoot.application.port.out.user.friend.RecommendFriendPort
import com.stark.shoot.domain.chat.user.User
import com.stark.shoot.infrastructure.annotation.UseCase
import io.github.oshai.kotlinlogging.KotlinLogging
import org.bson.types.ObjectId
import org.springframework.data.redis.core.StringRedisTemplate
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

@UseCase
class RecommendFriendService(
    private val recommendFriendPort: RecommendFriendPort,
    private val redisStringTemplate: StringRedisTemplate,
    private val objectMapper: ObjectMapper
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
            val json = redisStringTemplate.opsForValue().get(cacheKey)
            if (json != null) {
                objectMapper.readValue(json, object : TypeReference<List<User>>() {})
            } else null
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

}