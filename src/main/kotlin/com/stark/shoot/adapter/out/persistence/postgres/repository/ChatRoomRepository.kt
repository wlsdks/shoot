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

    /**
     * 사용자가 참여 중인 채팅방 ID 목록을 한 번의 쿼리로 조회
     * N+1 쿼리 문제를 해결하기 위한 최적화된 메서드
     *
     * @param userId 사용자 ID
     * @return 채팅방 ID 목록
     */
    @Query("""
        SELECT DISTINCT cru.chatRoom.id
        FROM ChatRoomUserEntity cru
        WHERE cru.user.id = :userId
        ORDER BY cru.chatRoom.lastActiveAt DESC
    """)
    fun findChatRoomIdsByUserId(@Param("userId") userId: Long): List<Long>

    /**
     * 여러 채팅방 ID로 채팅방 목록을 배치 조회하고 최근 활동 순으로 정렬
     *
     * @param chatRoomIds 채팅방 ID 목록
     * @return 채팅방 엔티티 목록 (최근 활동 순)
     */
    @Query("""
        SELECT cr
        FROM ChatRoomEntity cr
        WHERE cr.id IN :chatRoomIds
        ORDER BY cr.lastActiveAt DESC
    """)
    fun findAllByIdOrderByLastActiveAtDesc(@Param("chatRoomIds") chatRoomIds: List<Long>): List<ChatRoomEntity>

}