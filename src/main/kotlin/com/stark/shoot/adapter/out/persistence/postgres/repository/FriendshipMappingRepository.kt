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

    @Modifying
    @Query("""
        DELETE 
        FROM FriendshipMappingEntity f 
        WHERE f.user.id = :userId 
            AND f.friend.id = :friendId 
    """)
    fun deleteByUserIdAndFriendId(@Param("userId") userId: Long, @Param("friendId") friendId: Long)
}
