package com.stark.shoot.adapter.out.persistence.mongodb.mapper

import com.stark.shoot.adapter.out.persistence.mongodb.document.room.ChatRoomDocument
import com.stark.shoot.adapter.out.persistence.mongodb.document.room.embedded.ChatRoomMetadataDocument
import com.stark.shoot.adapter.out.persistence.mongodb.document.room.embedded.ChatRoomSettingsDocument
import com.stark.shoot.adapter.out.persistence.mongodb.document.room.embedded.ParticipantDocument
import com.stark.shoot.domain.chat.room.ChatRoom
import com.stark.shoot.domain.chat.room.ChatRoomMetadata
import com.stark.shoot.domain.chat.room.ChatRoomSettings
import com.stark.shoot.domain.chat.room.Participant
import org.bson.types.ObjectId
import org.springframework.stereotype.Component

@Component
class ChatRoomMapper {

    // 도메인 ChatRoom을 ChatRoomDocument로 변환
    fun toDocument(domain: ChatRoom): ChatRoomDocument {
        val participantIds = domain.participants.toMutableSet()
        val lastMsgId = domain.lastMessageId?.let { ObjectId(it) }
        return ChatRoomDocument(
            participants = participantIds,
            lastMessageId = lastMsgId,
            metadata = toMetadataDocument(domain.metadata),
            lastActiveAt = domain.lastActiveAt
        ).apply {
            id = domain.id?.let { ObjectId(it) }
        }
    }

    // ChatRoomDocument를 도메인 ChatRoom으로 변환
    fun toDomain(document: ChatRoomDocument): ChatRoom {
        return ChatRoom(
            id = document.id?.toString(),
            participants = document.participants.toMutableSet(),
            lastMessageId = document.lastMessageId?.toString(),
            metadata = toMetadata(document.metadata),
            lastActiveAt = document.lastActiveAt,
            createdAt = document.createdAt!!,
            updatedAt = document.updatedAt
        )
    }

    // ChatRoomMetadata -> ChatRoomMetadataDocument 변환
    private fun toMetadataDocument(metadata: ChatRoomMetadata): ChatRoomMetadataDocument {
        return ChatRoomMetadataDocument(
            title = metadata.title,
            type = metadata.type,
            participantsMetadata = metadata.participantsMetadata.entries.associate { (key, value) ->
                key to toParticipantDocument(value)
            },
            settings = toSettingsDocument(metadata.settings),
            announcement = metadata.announcement
        )
    }

    // ChatRoomMetadataDocument -> ChatRoomMetadata 변환
    private fun toMetadata(document: ChatRoomMetadataDocument): ChatRoomMetadata {
        return ChatRoomMetadata(
            title = document.title,
            type = document.type,
            participantsMetadata = document.participantsMetadata.entries.associate { (key, value) ->
                key to toParticipant(value)
            },
            settings = toSettings(document.settings),
            announcement = document.announcement
        )
    }

    private fun toParticipantDocument(participant: Participant): ParticipantDocument {
        return ParticipantDocument(
            lastReadMessageId = participant.lastReadMessageId?.let { ObjectId(it) },
            lastReadAt = participant.lastReadAt,
            unreadCount = participant.unreadCount,  // 수정: 0이 아니라 실제 unreadCount 사용
            joinedAt = participant.joinedAt,
            role = participant.role,
            nickname = participant.nickname,
            isActive = participant.isActive,
            isPinned = participant.isPinned,
            pinTimestamp = participant.pinTimestamp
        )
    }

    private fun toParticipant(document: ParticipantDocument): Participant {
        return Participant(
            lastReadMessageId = document.lastReadMessageId?.toString(),
            lastReadAt = document.lastReadAt,
            joinedAt = document.joinedAt,
            role = document.role,
            nickname = document.nickname,
            isActive = document.isActive,
            isPinned = document.isPinned,
            pinTimestamp = document.pinTimestamp,
            unreadCount = document.unreadCount  // 추가: 문서의 unreadCount를 도메인 객체에 포함
        )
    }

    private fun toSettingsDocument(settings: ChatRoomSettings): ChatRoomSettingsDocument {
        return ChatRoomSettingsDocument(
            isNotificationEnabled = settings.isNotificationEnabled,
            retentionDays = settings.retentionDays,
            isEncrypted = settings.isEncrypted,
            customSettings = settings.customSettings
        )
    }

    private fun toSettings(document: ChatRoomSettingsDocument): ChatRoomSettings {
        return ChatRoomSettings(
            isNotificationEnabled = document.isNotificationEnabled,
            retentionDays = document.retentionDays,
            isEncrypted = document.isEncrypted,
            customSettings = document.customSettings
        )
    }

}