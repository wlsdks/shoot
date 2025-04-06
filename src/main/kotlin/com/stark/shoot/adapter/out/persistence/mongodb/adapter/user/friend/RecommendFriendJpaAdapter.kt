package com.stark.shoot.adapter.out.persistence.mongodb.adapter.user.friend

import com.stark.shoot.adapter.out.persistence.postgres.entity.UserEntity
import com.stark.shoot.adapter.out.persistence.postgres.mapper.UserMapper
import com.stark.shoot.adapter.out.persistence.postgres.repository.FriendRequestRepository
import com.stark.shoot.adapter.out.persistence.postgres.repository.FriendshipMappingRepository
import com.stark.shoot.application.port.out.user.friend.RecommendFriendPort
import com.stark.shoot.domain.chat.user.User
import com.stark.shoot.infrastructure.annotation.Adapter
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext

@Adapter
class RecommendFriendJpaAdapter(
    private val friendshipMappingRepository: FriendshipMappingRepository,
    private val friendRequestRepository: FriendRequestRepository,
    private val userMapper: UserMapper
) : RecommendFriendPort {

    @PersistenceContext
    private lateinit var entityManager: EntityManager

    /**
     * 친구 추천 구현 (단순화된 버전)
     * 1. 친구의 친구들 추천 (상호 친구 수 기준으로 정렬)
     * 2. 상호 친구가 없을 경우 랜덤 유저 추천
     */
    override fun recommendFriends(
        userId: Long,
        limit: Int
    ): List<User> {
        // 1. 제외할 사용자 목록 준비 (본인, 친구, 친구 요청)
        val excludeIds = getExcludedUserIds(userId)

        // 2. 친구의 친구 추천 시도
        val recommendedUsers = findFriendsOfFriends(userId, excludeIds, limit)

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
     * 친구의 친구를 추천
     * - 상호 친구 수가 많은 순으로 정렬
     */
    private fun findFriendsOfFriends(
        userId: Long,
        excludeIds: Set<Long>,
        limit: Int
    ): MutableList<User> {
        val friendIds = friendshipMappingRepository.findAllByUserId(userId)
            .map { it.friend.id!! }

        // 친구가 없으면 빈 리스트 반환
        if (friendIds.isEmpty()) {
            return mutableListOf()
        }

        // 친구의 친구 조회 (JPQL 사용)
        val query = """
            SELECT f.friend, COUNT(f.user) as mutualCount
            FROM FriendshipMappingEntity f
            WHERE f.user.id IN :friendIds
                AND f.friend.id NOT IN :excludeIds
            GROUP BY f.friend
            ORDER BY mutualCount DESC, f.friend.id
        """

        val results = entityManager.createQuery(query)
            .setParameter("friendIds", friendIds)
            .setParameter("excludeIds", excludeIds)
            .setMaxResults(limit)
            .resultList

        return results.map { result ->
            val entity = (result as Array<*>)[0] as UserEntity
            val mutualCount = (result[1] as Number).toInt()
            userMapper.toDomain(entity)
        }.toMutableList()
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