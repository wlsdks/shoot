package com.stark.shoot.adapter.out.persistence.postgres.repository

import com.stark.shoot.adapter.out.persistence.postgres.entity.ChatRoomUserEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface ChatRoomUserRepository : JpaRepository<ChatRoomUserEntity, Long> {

    fun findByChatRoomIdAndUserId(chatRoomId: Long, userId: Long): Optional<ChatRoomUserEntity>

    fun findAllByChatRoomId(chatRoomId: Long): List<ChatRoomUserEntity>

    @Modifying
    @Query("""
        UPDATE ChatRoomUserEntity cru 
        SET cru.lastReadMessageId = :messageId 
        WHERE cru.chatRoom.id = :roomId 
            AND cru.user.id = :userId
    """)
    fun updateLastReadMessageId(
        @Param("roomId") roomId: Long,
        @Param("userId") userId: Long,
        @Param("messageId") messageId: String  // 타입 선언 변경
    )

    @Query("""
        SELECT cru.lastReadMessageId 
        FROM ChatRoomUserEntity cru 
        WHERE cru.chatRoom.id = :roomId 
            AND cru.user.id = :userId 
    """)
    fun findLastReadMessageId(
        @Param("roomId") roomId: Long,
        @Param("userId") userId: Long
    ): String?

}