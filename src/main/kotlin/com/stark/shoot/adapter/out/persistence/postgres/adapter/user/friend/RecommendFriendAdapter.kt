package com.stark.shoot.adapter.out.persistence.postgres.adapter.user.friend

import com.stark.shoot.adapter.out.persistence.postgres.entity.UserEntity
import com.stark.shoot.adapter.out.persistence.postgres.mapper.UserMapper
import com.stark.shoot.adapter.out.persistence.postgres.repository.FriendRequestRepository
import com.stark.shoot.adapter.out.persistence.postgres.repository.FriendshipMappingRepository
import com.stark.shoot.application.port.out.user.friend.RecommendFriendPort
import com.stark.shoot.domain.user.User
import com.stark.shoot.domain.user.type.FriendRequestStatus
import com.stark.shoot.domain.user.vo.UserId
import com.stark.shoot.infrastructure.annotation.Adapter
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import java.util.*

@Adapter
class RecommendFriendAdapter(
    private val friendshipMappingRepository: FriendshipMappingRepository,
    private val friendRequestRepository: FriendRequestRepository,
    private val userMapper: UserMapper
) : RecommendFriendPort {

    @PersistenceContext
    private lateinit var entityManager: EntityManager

    /**
     * 친구 추천 구현 (BFS 알고리즘 사용)
     * 1. BFS로 친구 네트워크를 탐색하여 추천
     * 2. 추천 결과가 충분하지 않으면 랜덤 유저로 채움
     */
    override fun recommendFriends(
        userId: UserId,
        limit: Int
    ): List<User> {
        // 특수 케이스: userId가 1이면 랜덤 유저만 반환 (이전에는 -1을 사용했으나 UserId는 양수만 허용)
        if (userId.value == 1L) {
            // 랜덤 유저 조회 시 제외할 ID는 없음 (자기 자신만 제외)
            val excludeIds = mutableSetOf<UserId>()
            return findRandomUsers(excludeIds, limit)
        }

        // 1. 제외할 사용자 목록 준비 (본인, 친구, 친구 요청)
        val excludeIds = getExcludedUserIds(userId)

        // 2. BFS로 친구 네트워크 탐색하여 추천
        val recommendedUsers = findFriendsByBFS(userId, excludeIds, limit)

        // 3. 추천 결과가 충분하지 않으면 랜덤 유저로 채움
        if (recommendedUsers.size < limit) {
            // 이미 추천된 사용자들의 ID를 제외 목록에 추가
            val updatedExcludeIds = excludeIds.toMutableSet()
            recommendedUsers.forEach { user ->
                user.id?.let { userId -> updatedExcludeIds.add(userId) }
            }

            val randomUsers = findRandomUsers(updatedExcludeIds, limit - recommendedUsers.size)
            recommendedUsers.addAll(randomUsers)
        }

        // 4. 최종 결과에서 현재 친구인 사용자 제외 (추가 검증, 양방향 관계 모두 확인)
        // 4.1 사용자가 추가한 친구들 (정방향 친구 관계)
        val outgoingFriendIds = friendshipMappingRepository.findAllByUserId(userId.value)
            .map { UserId.from(it.friend.id) }
            .toSet()

        // 4.2 사용자를 친구로 추가한 사용자들 (역방향 친구 관계)
        val incomingFriendIds = friendshipMappingRepository.findAllByFriendId(userId.value)
            .map { UserId.from(it.user.id) }
            .toSet()

        // 양방향 친구 관계를 합쳐서 전체 친구 목록 생성
        val allFriendIds = outgoingFriendIds.union(incomingFriendIds)

        return recommendedUsers.filter { user ->
            user.id?.let { userId -> !allFriendIds.contains(userId) } ?: true
        }
    }

    /**
     * 제외할 사용자 ID 목록 조회
     * - 본인
     * - 이미 친구인 사용자
     * - 친구 요청 중인 사용자
     */
    private fun getExcludedUserIds(userId: UserId): Set<UserId> {
        val excludeIds = mutableSetOf(userId)

        // 친구 목록 추가 (양방향 관계 모두 조회)
        // 1. 사용자가 추가한 친구들 (정방향 친구 관계)
        val outgoingFriendIds = friendshipMappingRepository.findAllByUserId(userId.value)
            .map { UserId.from(it.friend.id) }
        excludeIds.addAll(outgoingFriendIds)

        // 2. 사용자를 친구로 추가한 사용자들 (역방향 친구 관계)
        val incomingFriendIds = friendshipMappingRepository.findAllByFriendId(userId.value)
            .map { UserId.from(it.user.id) }
        excludeIds.addAll(incomingFriendIds)

        // 보낸 친구 요청 추가
        val outgoingRequestIds = friendRequestRepository
            .findAllBySenderIdAndStatus(userId.value, FriendRequestStatus.PENDING)
            .map { UserId.from(it.receiver.id) }
        excludeIds.addAll(outgoingRequestIds)

        // 받은 친구 요청 추가
        val incomingRequestIds = friendRequestRepository
            .findAllByReceiverIdAndStatus(userId.value, FriendRequestStatus.PENDING)
            .map { UserId.from(it.sender.id) }
        excludeIds.addAll(incomingRequestIds)

        return excludeIds
    }

    /**
     * BFS 알고리즘을 사용하여 친구 네트워크를 탐색하고 추천 친구를 찾음
     * - 가까운 거리의 사용자부터 추천 (친구의 친구, 친구의 친구의 친구 등)
     * - 각 거리 내에서는 상호 친구 수가 많은 순으로 정렬
     */
    private fun findFriendsByBFS(
        userId: UserId,
        excludeIds: Set<UserId>,
        limit: Int
    ): MutableList<User> {
        // 결과 저장 리스트
        val recommendedUsers = mutableListOf<User>()

        // 이미 방문한 사용자 ID 집합 (중복 방문 방지)
        val visited = mutableSetOf<UserId>()
        visited.addAll(excludeIds) // 제외 목록도 방문한 것으로 처리

        // BFS 큐 - Pair<사용자ID, 거리>
        val queue = LinkedList<Pair<UserId, Int>>()

        // 시작 사용자의 친구들을 큐에 추가 (거리 1)
        val directFriends = friendshipMappingRepository.findAllByUserId(userId.value)
            .map { it.friend.id!! }

        // 친구가 없으면 빈 리스트 반환하지 않고 랜덤 유저 추천
        if (directFriends.isEmpty()) {
            val randomUsers = findRandomUsers(excludeIds, limit)
            return randomUsers.toMutableList()
        }

        // 직접 친구들을 방문 처리하고 큐에 추가
        for (friendId in directFriends) {
            val friendUserId = UserId.from(friendId)
            visited.add(friendUserId)
            queue.add(Pair(friendUserId, 1))
        }

        // 현재 처리 중인 거리
        var currentDistance = 1
        // 현재 거리에서 찾은 추천 후보들
        val currentLevelCandidates = mutableListOf<Pair<UserEntity, Int>>()

        // BFS 탐색 시작
        while (queue.isNotEmpty() && recommendedUsers.size < limit) {
            val (currentUserId, distance) = queue.poll()

            // 거리가 변경되면 이전 거리의 후보들을 처리
            if (distance > currentDistance) {
                // 상호 친구 수로 정렬하여 추천 목록에 추가
                val sortedCandidates = currentLevelCandidates.sortedByDescending { it.second }
                for ((entity, _) in sortedCandidates) {
                    recommendedUsers.add(userMapper.toDomain(entity))
                    if (recommendedUsers.size >= limit) break
                }

                // 다음 거리로 이동
                currentLevelCandidates.clear()
                currentDistance = distance

                // 이미 충분한 추천을 찾았으면 종료
                if (recommendedUsers.size >= limit) break
            }

            // 현재 사용자의 친구들 조회
            val nextFriends = friendshipMappingRepository.findAllByUserId(currentUserId.value)

            for (friendship in nextFriends) {
                val nextIdLong = friendship.friend.id
                val nextId = UserId.from(nextIdLong)

                // 아직 방문하지 않은 사용자만 처리
                if (!visited.contains(nextId)) {
                    visited.add(nextId)

                    // 이미 친구인 사용자는 큐에 추가하지 않음
                    if (!excludeIds.contains(nextId)) {
                        // 다음 거리의 사용자는 큐에 추가
                        queue.add(Pair(nextId, distance + 1))

                        // 현재 거리의 사용자는 추천 후보로 추가
                        if (distance == currentDistance) {
                            // 상호 친구 수 계산
                            val mutualFriendCount = countMutualFriends(userId, nextId)

                            // 사용자 엔티티 조회
                            val userEntity = entityManager.find(UserEntity::class.java, nextIdLong)
                            if (userEntity != null) {
                                currentLevelCandidates.add(Pair(userEntity, mutualFriendCount))
                            }
                        }
                    }
                }
            }
        }

        // 마지막 거리의 후보들도 처리
        if (currentLevelCandidates.isNotEmpty() && recommendedUsers.size < limit) {
            val sortedCandidates = currentLevelCandidates.sortedByDescending { it.second }
            for ((entity, _) in sortedCandidates) {
                recommendedUsers.add(userMapper.toDomain(entity))
                if (recommendedUsers.size >= limit) break
            }
        }

        return recommendedUsers
    }

    /**
     * 두 사용자 간의 상호 친구 수를 계산
     */
    private fun countMutualFriends(
        userId1: UserId,
        userId2: UserId
    ): Int {
        val userId1Value = userId1.value
        val userId2Value = userId2.value

        val query = """
            SELECT COUNT(f1.friend.id)
            FROM FriendshipMappingEntity f1, FriendshipMappingEntity f2
            WHERE f1.user.id = :userId1Value
              AND f2.user.id = :userId2Value
              AND f1.friend.id = f2.friend.id
        """

        return entityManager.createQuery(query, Long::class.java)
            .setParameter("userId1Value", userId1Value)
            .setParameter("userId2Value", userId2Value)
            .singleResult.toInt()
    }

    /**
     * 랜덤 사용자 추천
     */
    private fun findRandomUsers(
        excludeIds: Set<UserId>,
        limit: Int
    ): List<User> {
        if (limit <= 0) {
            return emptyList()
        }

        val excludedIdValues = excludeIds.map { it.value } // UserId의 value 값만 추출

        // 함수 사용은 데이터베이스에 따라 다름 (PostgreSQL 예시)
        val query = """
            SELECT u FROM UserEntity u
            WHERE u.id NOT IN :excludedIdValues
            ORDER BY FUNCTION('RANDOM')
        """

        val randomEntities = entityManager.createQuery(query, UserEntity::class.java)
            .setParameter("excludedIdValues", excludedIdValues)
            .setMaxResults(limit)
            .resultList

        return randomEntities.map { userMapper.toDomain(it) }
    }

}
