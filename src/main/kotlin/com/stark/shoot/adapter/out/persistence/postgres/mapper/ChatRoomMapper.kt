package com.stark.shoot.adapter.out.persistence.postgres.mapper

import com.stark.shoot.adapter.out.persistence.postgres.entity.ChatRoomEntity
import com.stark.shoot.adapter.out.persistence.postgres.entity.ChatRoomUserEntity
import com.stark.shoot.domain.chat.message.vo.MessageId
import com.stark.shoot.domain.chatroom.ChatRoom
import com.stark.shoot.domain.chatroom.vo.ChatRoomAnnouncement
import com.stark.shoot.domain.chatroom.vo.ChatRoomId
import com.stark.shoot.domain.chatroom.vo.ChatRoomTitle
import com.stark.shoot.domain.user.vo.UserId
import org.springframework.stereotype.Component

@Component
class ChatRoomMapper {

    // 엔티티 -> 도메인 변환
    fun toDomain(
        entity: ChatRoomEntity,
        participants: List<ChatRoomUserEntity>
    ): ChatRoom {
        // 참여자 ID 목록
        val participantIds = participants.map { UserId.from(it.user.id) }.toMutableSet()

        // 고정된 참여자 ID 목록 (isPinned 필드 추가 필요)
        val pinnedParticipantIds = participants
            .filter { it.isPinned }
            .map { UserId.from(it.user.id) }
            .toMutableSet()

        // JPA 엔티티에서 바로 도메인 타입 사용
        val domainType = entity.type

        return ChatRoom(
            id = ChatRoomId.from(entity.id),
            title = entity.title?.let { ChatRoomTitle.from(it) },
            type = domainType,
            announcement = entity.announcement?.let { ChatRoomAnnouncement.from(it) },
            participants = participantIds,
            pinnedParticipants = pinnedParticipantIds,
            lastMessageId = entity.lastMessageId?.let { MessageId.from(it.toString()) },
            lastActiveAt = entity.lastActiveAt,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt
        )
    }

    // 도메인 -> 엔티티 변환 (ChatRoomUserEntity는 별도로 생성)
    fun toEntity(domain: ChatRoom): ChatRoomEntity {
        val lastMessageIdLong: Long? = domain.lastMessageId?.value?.toLongOrNull()

        return ChatRoomEntity(
            title = domain.title?.value,
            type = domain.type,
            announcement = domain.announcement?.value,
            lastMessageId = lastMessageIdLong,
            lastActiveAt = domain.lastActiveAt
        )
    }

}