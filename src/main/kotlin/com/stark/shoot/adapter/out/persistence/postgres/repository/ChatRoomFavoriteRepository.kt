package com.stark.shoot.adapter.out.persistence.postgres.repository

import com.stark.shoot.adapter.out.persistence.postgres.entity.ChatRoomFavoriteEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query

interface ChatRoomFavoriteRepository : JpaRepository<ChatRoomFavoriteEntity, Long> {

    /**
     * 사용자의 특정 채팅방 즐겨찾기를 조회합니다.
     */
    fun findByUserIdAndChatRoomId(userId: Long, chatRoomId: Long): ChatRoomFavoriteEntity?

    /**
     * 사용자의 모든 즐겨찾기를 조회합니다 (displayOrder, pinnedAt 기준 정렬).
     */
    @Query("""
        SELECT f FROM ChatRoomFavoriteEntity f
        WHERE f.userId = :userId
        ORDER BY
            CASE WHEN f.displayOrder IS NULL THEN 1 ELSE 0 END,
            f.displayOrder ASC,
            f.pinnedAt DESC
    """)
    fun findAllByUserId(userId: Long): List<ChatRoomFavoriteEntity>

    /**
     * 사용자의 고정된 즐겨찾기만 조회합니다.
     */
    @Query("""
        SELECT f FROM ChatRoomFavoriteEntity f
        WHERE f.userId = :userId AND f.isPinned = true
        ORDER BY
            CASE WHEN f.displayOrder IS NULL THEN 1 ELSE 0 END,
            f.displayOrder ASC,
            f.pinnedAt DESC
    """)
    fun findAllPinnedByUserId(userId: Long): List<ChatRoomFavoriteEntity>

    /**
     * 사용자의 고정된 즐겨찾기 개수를 조회합니다.
     */
    fun countByUserIdAndIsPinned(userId: Long, isPinned: Boolean = true): Long

    /**
     * 즐겨찾기 존재 여부를 확인합니다.
     */
    fun existsByUserIdAndChatRoomId(userId: Long, chatRoomId: Long): Boolean

    /**
     * 채팅방이 즐겨찾기 되어있는지 확인합니다 (누구라도).
     */
    fun existsByChatRoomId(chatRoomId: Long): Boolean

    /**
     * 사용자의 특정 채팅방 즐겨찾기를 삭제합니다.
     */
    @Modifying
    @Query("DELETE FROM ChatRoomFavoriteEntity f WHERE f.userId = :userId AND f.chatRoomId = :chatRoomId")
    fun deleteByUserIdAndChatRoomId(userId: Long, chatRoomId: Long)

    /**
     * 사용자의 모든 즐겨찾기를 삭제합니다.
     */
    @Modifying
    @Query("DELETE FROM ChatRoomFavoriteEntity f WHERE f.userId = :userId")
    fun deleteAllByUserId(userId: Long)

    /**
     * 특정 채팅방의 모든 즐겨찾기를 삭제합니다.
     */
    @Modifying
    @Query("DELETE FROM ChatRoomFavoriteEntity f WHERE f.chatRoomId = :chatRoomId")
    fun deleteAllByChatRoomId(chatRoomId: Long)
}
