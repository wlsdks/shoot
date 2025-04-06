package com.stark.shoot.adapter.out.persistence.postgres.repository

import com.stark.shoot.adapter.out.persistence.postgres.entity.ChatRoomEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface ChatRoomRepository : JpaRepository<ChatRoomEntity, Long> {

    @Query("""
        SELECT cr 
        FROM ChatRoomEntity cr 
        JOIN ChatRoomUserEntity cru 
            ON cr.id = cru.chatRoom.id 
        WHERE cru.user.id = :userId
    """)
    fun findByParticipant(@Param("userId") userId: Long): List<ChatRoomEntity>

    @Query("""
        SELECT cr 
        FROM ChatRoomEntity cr 
        JOIN ChatRoomUserEntity cru 
            ON cr.id = cru.chatRoom.id 
        WHERE cru.user.id = :userId 
            AND cru.isPinned = true
    """)
    fun findPinnedRoomsByUserId(@Param("userId") userId: Long): List<ChatRoomEntity>

}