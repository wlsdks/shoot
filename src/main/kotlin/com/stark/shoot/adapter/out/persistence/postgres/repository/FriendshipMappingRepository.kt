package com.stark.shoot.adapter.out.persistence.postgres.repository

import com.stark.shoot.adapter.out.persistence.postgres.entity.FriendshipMappingEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface FriendshipMappingRepository : JpaRepository<FriendshipMappingEntity, Long> {
    fun findAllByUserId(userId: Long): List<FriendshipMappingEntity>

    // 사용자가 친구로 등록된 관계 조회 (역방향 친구 관계)
    fun findAllByFriendId(friendId: Long): List<FriendshipMappingEntity>

    fun existsByUserIdAndFriendId(userId: Long, friendId: Long): Boolean

    /**
     * 특정 사용자와 여러 대상 사용자 간의 친구 관계를 배치 조회
     * N+1 쿼리 문제를 방지하기 위한 배치 조회
     *
     * @param userId 기준 사용자 ID
     * @param friendIds 확인할 친구 ID 목록
     * @return 친구 관계 목록
     */
    fun findAllByUserIdAndFriendIdIn(userId: Long, friendIds: List<Long>): List<FriendshipMappingEntity>

    @Modifying
    @Query("""
        DELETE
        FROM FriendshipMappingEntity f
        WHERE f.user.id = :userId
            AND f.friend.id = :friendId
    """)
    fun deleteByUserIdAndFriendId(@Param("userId") userId: Long, @Param("friendId") friendId: Long)

    @Modifying
    @Query("""
        DELETE
        FROM FriendshipMappingEntity f
        WHERE f.user.id = :userId
    """)
    fun deleteByUserId(@Param("userId") userId: Long)

    @Modifying
    @Query("""
        DELETE
        FROM FriendshipMappingEntity f
        WHERE f.friend.id = :friendId
    """)
    fun deleteByFriendId(@Param("friendId") friendId: Long)
}
