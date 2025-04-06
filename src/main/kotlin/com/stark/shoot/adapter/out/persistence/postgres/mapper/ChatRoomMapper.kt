package com.stark.shoot.adapter.out.persistence.postgres.mapper

import com.stark.shoot.adapter.out.persistence.postgres.entity.ChatRoomEntity
import com.stark.shoot.domain.chat.room.ChatRoom
import org.springframework.stereotype.Component

@Component
class ChatRoomMapper {

    // 엔티티 -> 도메인 변환
    fun toDomain(entity: ChatRoomEntity): ChatRoom {
        return ChatRoom(
            id = entity.id.toString(),
            title = entity.title,
            type = entity.type,
            announcement = entity.announcement,
            participants = entity.participantIds.toMutableSet(),
            pinnedParticipants = entity.pinnedParticipantIds.toMutableSet(),
            lastMessageId = entity.lastMessageId?.toString(),
            lastActiveAt = entity.lastActiveAt,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt
        )
    }

    // 도메인 -> 엔티티 변환
    fun toEntity(domain: ChatRoom): ChatRoomEntity {
        val lastMessageIdLong: Long? = domain.lastMessageId?.toLongOrNull()
        return ChatRoomEntity(
            title = domain.title,
            type = domain.type,
            announcement = domain.announcement,
            participantIds = domain.participants.toList(),
            pinnedParticipantIds = domain.pinnedParticipants.toList(), // 생성자에 포함
            lastMessageId = lastMessageIdLong,
            lastActiveAt = domain.lastActiveAt
        )
    }

}
