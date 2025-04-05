package com.stark.shoot.adapter.out.persistence.postgres.repository

import com.stark.shoot.adapter.out.persistence.postgres.entity.ChatRoomEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface ChatRoomRepository : JpaRepository<ChatRoomEntity, Long> {
    fun findByParticipantIds(participantIds: MutableList<Long>): MutableList<ChatRoomEntity>

    @Query(
        value = """
            SELECT * 
            FROM chat_rooms 
            WHERE pinned_participant_ids @> to_jsonb(:userId::bigint)
        """,
        nativeQuery = true
    )
    fun findPinnedRoomsByUserId(@Param("userId") userId: Long): List<ChatRoomEntity>
}