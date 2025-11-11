package com.stark.shoot.adapter.out.persistence.postgres.mapper

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.stark.shoot.adapter.out.persistence.postgres.entity.ChatRoomEntity
import com.stark.shoot.adapter.out.persistence.postgres.entity.ChatRoomUserEntity
import com.stark.shoot.domain.chatroom.ChatRoom
import com.stark.shoot.domain.chatroom.ChatRoomSettings
import com.stark.shoot.domain.chatroom.vo.ChatRoomAnnouncement
import com.stark.shoot.domain.chatroom.vo.ChatRoomId
import com.stark.shoot.domain.chatroom.vo.ChatRoomTitle
import com.stark.shoot.domain.chatroom.vo.RetentionDays
import com.stark.shoot.domain.shared.UserId
import org.springframework.stereotype.Component

@Component
class ChatRoomMapper(
    private val objectMapper: ObjectMapper
) {

    // 엔티티 -> 도메인 변환
    fun toDomain(
        entity: ChatRoomEntity,
        participants: List<ChatRoomUserEntity>
    ): ChatRoom {
        // 참여자 ID 목록
        val participantIds = participants.map { UserId.from(it.userId) }.toMutableSet()

        // JPA 엔티티에서 바로 도메인 타입 사용
        val domainType = entity.type

        // ChatRoomSettings 변환
        val customSettingsMap: Map<String, Any> = entity.customSettings?.let {
            try {
                objectMapper.readValue(it)
            } catch (e: Exception) {
                emptyMap()
            }
        } ?: emptyMap()

        val settings = ChatRoomSettings(
            isNotificationEnabled = entity.isNotificationEnabled,
            retentionDays = entity.retentionDays?.let { RetentionDays.from(it) },
            isEncrypted = entity.isEncrypted,
            customSettings = customSettingsMap
        )

        return ChatRoom(
            id = ChatRoomId.from(entity.id),
            title = entity.title?.let { ChatRoomTitle.from(it) },
            type = domainType,
            announcement = entity.announcement?.let { ChatRoomAnnouncement.from(it) },
            participants = participantIds,
            lastMessageId = entity.lastMessageId?.let { com.stark.shoot.domain.chatroom.vo.MessageId.from(it.toString()) },
            lastActiveAt = entity.lastActiveAt,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt,
            settings = settings
        )
    }

    // 도메인 -> 엔티티 변환 (ChatRoomUserEntity는 별도로 생성)
    fun toEntity(domain: ChatRoom): ChatRoomEntity {
        val lastMessageIdLong: Long? = domain.lastMessageId?.value?.toLongOrNull()

        // ChatRoomSettings 변환
        val customSettingsJson: String? = if (domain.settings.customSettings.isNotEmpty()) {
            try {
                objectMapper.writeValueAsString(domain.settings.customSettings)
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }

        return ChatRoomEntity(
            title = domain.title?.value,
            type = domain.type,
            announcement = domain.announcement?.value,
            lastMessageId = lastMessageIdLong,
            lastActiveAt = domain.lastActiveAt,
            isNotificationEnabled = domain.settings.isNotificationEnabled,
            retentionDays = domain.settings.retentionDays?.value,
            isEncrypted = domain.settings.isEncrypted,
            customSettings = customSettingsJson
        )
    }

}