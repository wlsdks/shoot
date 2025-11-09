package com.stark.shoot.application.service.user.friend.recommend

import com.stark.shoot.adapter.`in`.rest.dto.social.friend.FriendResponse
import com.stark.shoot.application.port.`in`.user.friend.RecommendFriendsUseCase
import com.stark.shoot.application.port.`in`.user.friend.command.GetRecommendedFriendsCommand
import com.stark.shoot.application.port.out.user.UserQueryPort
import com.stark.shoot.application.port.out.user.friend.RecommendFriendPort
import com.stark.shoot.domain.user.User
import com.stark.shoot.domain.shared.UserId
import com.stark.shoot.infrastructure.annotation.UseCase
import io.github.oshai.kotlinlogging.KotlinLogging
import java.util.concurrent.ConcurrentHashMap

/**
 * 친구 추천 서비스
 *
 * BFS 알고리즘 기반 친구 추천 기능을 제공합니다.
 * 캐시 관리는 FriendRecommendationCacheManager에 위임합니다.
 */
@UseCase
class RecommendFriendService(
    private val recommendFriendPort: RecommendFriendPort,
    private val userQueryPort: UserQueryPort,
    private val cacheManager: FriendRecommendationCacheManager
) : RecommendFriendsUseCase {

    private val logger = KotlinLogging.logger {}

    // 현재 계산 중인 사용자 ID 목록 (중복 계산 방지)
    private val inProgressUsers = ConcurrentHashMap.newKeySet<String>()

    /**
     * 친구 추천 구현
     * - 캐싱 적용
     * - 페이징 지원
     *
     * @param command 친구 추천 조회 커맨드
     * @return 추천 친구 목록
     */
    override fun getRecommendedFriends(
        command: GetRecommendedFriendsCommand
    ): List<FriendResponse> {
        val userId = command.userId
        val skip = command.skip
        val limit = command.limit

        // 캐시 키 생성
        val cacheKey = cacheManager.getCacheKey(userId, limit)

        // 캐시 확인
        val cachedUsers = cacheManager.getFromCache(cacheKey)

        // 캐시 데이터가 있으면 추가 필터링 후 페이징해서 반환
        if (cachedUsers != null) {
            val filteredUsers = filterExistingRelationships(userId, cachedUsers)
            return paginateAndConvert(filteredUsers, skip, limit)
        }

        // 중복 계산 방지
        val userIdStr = userId.toString()
        if (inProgressUsers.contains(userIdStr)) {
            logger.info { "이미 추천 목록 계산 중: userId=$userId, 랜덤 유저 반환" }
            return getFallbackRecommendations(userId, skip, limit)
        }

        try {
            inProgressUsers.add(userIdStr)

            // 데이터베이스에서 추천 친구 계산 및 캐싱
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
     * 폴백 추천 목록 반환 (중복 계산 방지 시)
     */
    private fun getFallbackRecommendations(userId: UserId, skip: Int, limit: Int): List<FriendResponse> {
        val randomRecommendations = recommendFriendPort.recommendFriends(UserId.from(1), limit * 2)
        val filteredRandomUsers = filterExistingRelationships(userId, randomRecommendations)
        return paginateAndConvert(filteredRandomUsers, skip, limit)
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
        userId: UserId,
        cacheKey: String,
        limit: Int
    ): List<User> {
        var recommendedUsers = recommendFriendPort.recommendFriends(userId, limit * 2)
            .filter { it.id != userId } // 본인 제외

        // 추천 결과가 없으면 랜덤 유저로 대체
        if (recommendedUsers.isEmpty()) {
            logger.info { "추천 친구가 없음: userId=$userId, 랜덤 유저 반환" }
            recommendedUsers = recommendFriendPort.recommendFriends(UserId.from(1), limit * 2)
                .filter { it.id != userId } // 본인 제외
        }

        logger.debug { "추천 친구 계산 결과: userId=$userId, 결과=${recommendedUsers.size}명" }

        // 캐시에 저장 (Redis + 로컬)
        cacheManager.saveToCache(cacheKey, recommendedUsers)

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
                    id = user.id?.value ?: 0L,
                    username = user.username.value,
                    nickname = user.nickname.value,
                    profileImageUrl = user.profileImageUrl?.value ?: "",
                )
            }
    }

    /**
     * 이미 친구인 사용자와 친구 요청을 보낸 사용자를 필터링합니다.
     * N+1 쿼리 문제를 해결하기 위해 배치 조회를 사용합니다.
     *
     * @param userId 현재 사용자 ID
     * @param users 필터링할 사용자 목록
     * @return 필터링된 사용자 목록
     */
    private fun filterExistingRelationships(
        userId: UserId,
        users: List<User>
    ): List<User> {
        if (users.isEmpty()) return emptyList()

        try {
            // 모든 사용자 ID 추출
            val userIds = users.mapNotNull { it.id }

            // 배치 조회로 친구 관계 확인 (N+1 문제 해결)
            val friendIds = userQueryPort.checkFriendshipBatch(userId, userIds)
            val outgoingRequestIds = userQueryPort.checkOutgoingFriendRequestBatch(userId, userIds)
            val incomingRequestIds = userQueryPort.checkIncomingFriendRequestBatch(userId, userIds)

            // 제외할 사용자 ID 집합
            val excludedIds = friendIds + outgoingRequestIds + incomingRequestIds

            // 필터링된 목록 반환
            return users.filter { user ->
                user.id?.let { id -> !excludedIds.contains(id) } ?: true
            }
        } catch (e: Exception) {
            logger.warn(e) { "친구 관계 확인 중 오류 발생: userId=${userId.value}" }
            // 오류 발생 시 원본 목록 반환
            return users
        }
    }

    /**
     * 특정 사용자의 추천 친구 캐시를 무효화합니다.
     * 이 메서드는 친구 요청이 발생했을 때 호출되어 캐시를 갱신합니다.
     *
     * @param userId 캐시를 무효화할 사용자 ID
     */
    fun invalidateUserCache(userId: UserId) {
        cacheManager.invalidateUserCache(userId)
    }

}
