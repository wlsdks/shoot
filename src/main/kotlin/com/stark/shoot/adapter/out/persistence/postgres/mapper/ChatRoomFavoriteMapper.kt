package com.stark.shoot.adapter.out.persistence.postgres.mapper

import com.stark.shoot.adapter.out.persistence.postgres.entity.ChatRoomFavoriteEntity
import com.stark.shoot.domain.chatroom.favorite.ChatRoomFavorite
import com.stark.shoot.domain.chatroom.favorite.vo.ChatRoomFavoriteId
import com.stark.shoot.domain.chatroom.vo.ChatRoomId
import com.stark.shoot.domain.shared.UserId
import org.springframework.stereotype.Component

@Component
class ChatRoomFavoriteMapper {

    /**
     * Entity -> Domain 변환
     */
    fun toDomain(entity: ChatRoomFavoriteEntity): ChatRoomFavorite {
        return ChatRoomFavorite(
            id = ChatRoomFavoriteId.from(entity.id),
            userId = UserId.from(entity.userId),
            chatRoomId = ChatRoomId.from(entity.chatRoomId),
            isPinned = entity.isPinned,
            pinnedAt = entity.pinnedAt,
            displayOrder = entity.displayOrder,
            createdAt = entity.createdAt
        )
    }

    /**
     * Domain -> Entity 변환
     */
    fun toEntity(domain: ChatRoomFavorite): ChatRoomFavoriteEntity {
        return ChatRoomFavoriteEntity(
            userId = domain.userId.value,
            chatRoomId = domain.chatRoomId.value,
            isPinned = domain.isPinned,
            pinnedAt = domain.pinnedAt,
            displayOrder = domain.displayOrder
        )
    }
}
