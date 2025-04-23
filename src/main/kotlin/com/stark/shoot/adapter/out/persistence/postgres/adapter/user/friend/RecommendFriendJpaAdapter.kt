package com.stark.shoot.adapter.out.persistence.postgres.adapter.user.friend

import com.stark.shoot.adapter.out.persistence.postgres.entity.UserEntity
import com.stark.shoot.adapter.out.persistence.postgres.mapper.UserMapper
import com.stark.shoot.adapter.out.persistence.postgres.repository.FriendRequestRepository
import com.stark.shoot.adapter.out.persistence.postgres.repository.FriendshipMappingRepository
import com.stark.shoot.application.port.out.user.friend.RecommendFriendPort
import com.stark.shoot.domain.chat.user.User
import com.stark.shoot.infrastructure.annotation.Adapter
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import java.util.*

@Adapter
class RecommendFriendJpaAdapter(
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
        userId: Long,
        limit: Int
    ): List<User> {
        // 1. 제외할 사용자 목록 준비 (본인, 친구, 친구 요청)
        val excludeIds = getExcludedUserIds(userId)

        // 2. BFS로 친구 네트워크 탐색하여 추천
        val recommendedUsers = findFriendsByBFS(userId, excludeIds, limit)

        // 3. 추천 결과가 충분하지 않으면 랜덤 유저로 채움
        if (recommendedUsers.size < limit) {
            val randomUsers = findRandomUsers(excludeIds, limit - recommendedUsers.size)
            recommendedUsers.addAll(randomUsers)
        }

        return recommendedUsers
    }

    /**
     * 제외할 사용자 ID 목록 조회
     * - 본인
     * - 이미 친구인 사용자
     * - 친구 요청 중인 사용자
     */
    private fun getExcludedUserIds(userId: Long): Set<Long> {
        val excludeIds = mutableSetOf(userId)

        // 친구 목록 추가
        val friendIds = friendshipMappingRepository.findAllByUserId(userId)
            .map { it.friend.id!! }
        excludeIds.addAll(friendIds)

        // 보낸 친구 요청 추가
        val outgoingRequestIds = friendRequestRepository.findAllBySenderId(userId)
            .map { it.receiver.id!! }
        excludeIds.addAll(outgoingRequestIds)

        // 받은 친구 요청 추가
        val incomingRequestIds = friendRequestRepository.findAllByReceiverId(userId)
            .map { it.sender.id!! }
        excludeIds.addAll(incomingRequestIds)

        return excludeIds
    }

    /**
     * BFS 알고리즘을 사용하여 친구 네트워크를 탐색하고 추천 친구를 찾음
     * - 가까운 거리의 사용자부터 추천 (친구의 친구, 친구의 친구의 친구 등)
     * - 각 거리 내에서는 상호 친구 수가 많은 순으로 정렬
     */
    private fun findFriendsByBFS(
        userId: Long,
        excludeIds: Set<Long>,
        limit: Int
    ): MutableList<User> {
        // 결과 저장 리스트
        val recommendedUsers = mutableListOf<User>()

        // 이미 방문한 사용자 ID 집합 (중복 방문 방지)
        val visited = mutableSetOf<Long>()
        visited.addAll(excludeIds) // 제외 목록도 방문한 것으로 처리

        // BFS 큐 - Pair<사용자ID, 거리>
        val queue = LinkedList<Pair<Long, Int>>()

        // 시작 사용자의 친구들을 큐에 추가 (거리 1)
        val directFriends = friendshipMappingRepository.findAllByUserId(userId)
            .map { it.friend.id!! }

        // 친구가 없으면 빈 리스트 반환
        if (directFriends.isEmpty()) {
            return mutableListOf()
        }

        // 직접 친구들을 방문 처리하고 큐에 추가
        for (friendId in directFriends) {
            visited.add(friendId)
            queue.add(Pair(friendId, 1))
        }

        // 현재 처리 중인 거리
        var currentDistance = 1
        // 현재 거리에서 찾은 추천 후보들
        val currentLevelCandidates = mutableListOf<Pair<UserEntity, Int>>()

        // BFS 탐색 시작
        while (queue.isNotEmpty() && recommendedUsers.size < limit) {
            val (currentId, distance) = queue.poll()

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
            val nextFriends = friendshipMappingRepository.findAllByUserId(currentId)

            for (friendship in nextFriends) {
                val nextId = friendship.friend.id!!

                // 아직 방문하지 않은 사용자만 처리
                if (!visited.contains(nextId)) {
                    visited.add(nextId)

                    // 다음 거리의 사용자는 큐에 추가
                    queue.add(Pair(nextId, distance + 1))

                    // 현재 거리의 사용자는 추천 후보로 추가
                    if (distance == currentDistance && !excludeIds.contains(nextId)) {
                        // 상호 친구 수 계산
                        val mutualFriendCount = countMutualFriends(userId, nextId)

                        // 사용자 엔티티 조회
                        val userEntity = entityManager.find(UserEntity::class.java, nextId)
                        if (userEntity != null) {
                            currentLevelCandidates.add(Pair(userEntity, mutualFriendCount))
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
    private fun countMutualFriends(userId1: Long, userId2: Long): Int {
        val query = """
            SELECT COUNT(f1.friend.id)
            FROM FriendshipMappingEntity f1, FriendshipMappingEntity f2
            WHERE f1.user.id = :userId1
              AND f2.user.id = :userId2
              AND f1.friend.id = f2.friend.id
        """

        return entityManager.createQuery(query, Long::class.java)
            .setParameter("userId1", userId1)
            .setParameter("userId2", userId2)
            .singleResult.toInt()
    }

    /**
     * 랜덤 사용자 추천
     */
    private fun findRandomUsers(
        excludeIds: Set<Long>,
        limit: Int
    ): List<User> {
        if (limit <= 0) {
            return emptyList()
        }

        // 함수 사용은 데이터베이스에 따라 다름 (PostgreSQL 예시)
        val query = """
            SELECT u FROM UserEntity u
            WHERE u.id NOT IN :excludeIds
            ORDER BY FUNCTION('RANDOM')
        """

        val randomEntities = entityManager.createQuery(query, UserEntity::class.java)
            .setParameter("excludeIds", excludeIds)
            .setMaxResults(limit)
            .resultList

        return randomEntities.map { userMapper.toDomain(it) }
    }

}
