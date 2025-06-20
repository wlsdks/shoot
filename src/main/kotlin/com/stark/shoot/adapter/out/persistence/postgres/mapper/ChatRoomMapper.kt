package com.stark.shoot.adapter.out.persistence.postgres.mapper

import com.stark.shoot.adapter.out.persistence.postgres.entity.ChatRoomEntity
import com.stark.shoot.adapter.out.persistence.postgres.entity.ChatRoomUserEntity
import com.stark.shoot.domain.chat.room.ChatRoom
import com.stark.shoot.domain.chat.room.ChatRoomType
import com.stark.shoot.domain.chat.room.ChatRoomTitle
import com.stark.shoot.domain.chat.room.ChatRoomAnnouncement
import org.springframework.stereotype.Component

@Component
class ChatRoomMapper {

    // 엔티티 -> 도메인 변환
    fun toDomain(
        entity: ChatRoomEntity,
        participants: List<ChatRoomUserEntity>
    ): ChatRoom {
        // 참여자 ID 목록
        val participantIds = participants.map { it.user.id }.toMutableSet()

        // 고정된 참여자 ID 목록 (isPinned 필드 추가 필요)
        val pinnedParticipantIds = participants
            .filter { it.isPinned }
            .map { it.user.id }
            .toMutableSet()

        // JPA 엔티티에서 바로 도메인 타입 사용
        val domainType = entity.type

        return ChatRoom(
            id = entity.id,
            title = entity.title?.let { ChatRoomTitle.from(it) },
            type = domainType,
            announcement = entity.announcement?.let { ChatRoomAnnouncement.from(it) },
            participants = participantIds,
            pinnedParticipants = pinnedParticipantIds,
            lastMessageId = entity.lastMessageId?.toString(),
            lastActiveAt = entity.lastActiveAt,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt
        )
    }

    // 도메인 -> 엔티티 변환 (ChatRoomUserEntity는 별도로 생성)
    fun toEntity(domain: ChatRoom): ChatRoomEntity {
        val lastMessageIdLong: Long? = domain.lastMessageId?.toLongOrNull()

        return ChatRoomEntity(
            title = domain.title?.value,
            type = domain.type,
            announcement = domain.announcement?.value,
            lastMessageId = lastMessageIdLong,
            lastActiveAt = domain.lastActiveAt
        )
    }

}
