package com.stark.shoot.adapter.out.persistence.postgres.repository

import com.stark.shoot.adapter.out.persistence.postgres.entity.ChatRoomUserEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository


@Repository
interface ChatRoomUserRepository : JpaRepository<ChatRoomUserEntity, Long> {

    @Modifying
    @Query(
        """
        UPDATE ChatRoomUserEntity cru 
        SET cru.lastReadMessageId = :messageId 
        WHERE cru.chatRoom.id = :roomId 
            AND cru.user.id = :userId
    """
    )
    fun updateLastReadMessageId(
        @Param("roomId") roomId: Long,
        @Param("userId") userId: Long,
        @Param("messageId") messageId: String
    )

    @Query(
        """
        SELECT cru.lastReadMessageId 
        FROM ChatRoomUserEntity cru 
        WHERE cru.chatRoom.id = :roomId 
            AND cru.user.id = :userId 
    """
    )
    fun findLastReadMessageId(
        @Param("roomId") roomId: Long,
        @Param("userId") userId: Long
    ): String?

    fun findByUserIdAndIsPinnedTrue(userId: Long): List<ChatRoomUserEntity>
    fun findByChatRoomId(chatRoomId: Long): List<ChatRoomUserEntity>
    fun findByUserId(userId: Long): List<ChatRoomUserEntity>

    /**
     * 여러 채팅방의 참여자들을 한 번의 쿼리로 조회 (N+1 문제 해결)
     *
     * @param chatRoomIds 채팅방 ID 목록
     * @return 해당 채팅방들의 모든 참여자 목록
     */
    @Query(
        """
        SELECT cru
        FROM ChatRoomUserEntity cru
        JOIN FETCH cru.user
        WHERE cru.chatRoom.id IN :chatRoomIds
    """
    )
    fun findAllByChatRoomIds(@Param("chatRoomIds") chatRoomIds: List<Long>): List<ChatRoomUserEntity>

}