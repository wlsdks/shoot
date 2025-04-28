package com.stark.shoot.adapter.out.persistence.postgres.mapper

import com.stark.shoot.adapter.out.persistence.postgres.entity.ChatRoomEntity
import com.stark.shoot.adapter.out.persistence.postgres.entity.ChatRoomUserEntity
import com.stark.shoot.adapter.out.persistence.postgres.entity.enumerate.ChatRoomType as PersistenceChatRoomType
import com.stark.shoot.domain.chat.room.ChatRoom
import com.stark.shoot.domain.chat.room.ChatRoomType as DomainChatRoomType
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

        // 영속성 타입에서 도메인 타입으로 변환
        val domainType = when (entity.type) {
            PersistenceChatRoomType.INDIVIDUAL -> DomainChatRoomType.INDIVIDUAL
            PersistenceChatRoomType.GROUP -> DomainChatRoomType.GROUP
        }

        return ChatRoom(
            id = entity.id,
            title = entity.title,
            type = domainType,
            announcement = entity.announcement,
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

        // 도메인 타입에서 영속성 타입으로 변환
        val persistenceType = when (domain.type) {
            DomainChatRoomType.INDIVIDUAL -> PersistenceChatRoomType.INDIVIDUAL
            DomainChatRoomType.GROUP -> PersistenceChatRoomType.GROUP
        }

        return ChatRoomEntity(
            title = domain.title,
            type = persistenceType,
            announcement = domain.announcement,
            lastMessageId = lastMessageIdLong,
            lastActiveAt = domain.lastActiveAt
        )
    }

}
