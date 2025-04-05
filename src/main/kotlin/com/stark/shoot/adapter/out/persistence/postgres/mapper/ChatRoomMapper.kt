package com.stark.shoot.adapter.out.persistence.postgres.mapper

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.stark.shoot.adapter.out.persistence.postgres.entity.ChatRoomEntity
import com.stark.shoot.adapter.out.persistence.postgres.entity.ChatRoomSettingsEntity
import com.stark.shoot.domain.chat.room.ChatRoom
import com.stark.shoot.domain.chat.room.ChatRoomSettings
import org.springframework.stereotype.Component

@Component
class ChatRoomMapper {

    private val objectMapper = jacksonObjectMapper()

    // 엔티티 -> 도메인 변환
    fun toDomain(entity: ChatRoomEntity): ChatRoom {
        return ChatRoom(
            id = entity.id.toString(),
            title = entity.title,
            type = entity.type,
            settings = toDomain(entity.settings),
            announcement = entity.announcement,
            participants = entity.participantIds.toMutableSet(),
            pinnedParticipants = entity.pinnedParticipantIds.toMutableSet(),
            lastMessageId = entity.lastMessageId?.toString(),
            lastMessageText = null,  // 엔티티에는 메시지 텍스트 정보가 없으므로
            lastActiveAt = entity.lastActiveAt,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt
        )
    }

    // 엔티티 ChatRoomSettingsEntity -> 도메인 ChatRoomSettings 변환
    fun toDomain(settingsEntity: ChatRoomSettingsEntity): ChatRoomSettings {
        val customSettings: Map<String, Any> = settingsEntity.customSettingsJson?.let {
            try {
                objectMapper.readValue(it)
            } catch (e: Exception) {
                emptyMap()
            }
        } ?: emptyMap()

        return ChatRoomSettings(
            isNotificationEnabled = settingsEntity.isNotificationEnabled,
            retentionDays = settingsEntity.retentionDays,
            isEncrypted = settingsEntity.isEncrypted,
            customSettings = customSettings
        )
    }

    // 도메인 -> 엔티티 변환
    fun toEntity(domain: ChatRoom): ChatRoomEntity {
        val lastMessageIdLong: Long? = domain.lastMessageId?.toLongOrNull()
        return ChatRoomEntity(
            title = domain.title,
            type = domain.type,
            settings = toEntity(domain.settings),
            announcement = domain.announcement,
            participantIds = domain.participants.toList(),
            pinnedParticipantIds = domain.pinnedParticipants.toList(), // 생성자에 포함
            lastMessageId = lastMessageIdLong,
            lastActiveAt = domain.lastActiveAt
        )
    }

    // 도메인 ChatRoomSettings -> 엔티티 ChatRoomSettingsEntity 변환
    fun toEntity(domain: ChatRoomSettings): ChatRoomSettingsEntity {
        val customSettingsJson = if (domain.customSettings.isNotEmpty())
            objectMapper.writeValueAsString(domain.customSettings)
        else null

        return ChatRoomSettingsEntity(
            isNotificationEnabled = domain.isNotificationEnabled,
            retentionDays = domain.retentionDays,
            isEncrypted = domain.isEncrypted,
            customSettingsJson = customSettingsJson
        )
    }

}
