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

    // ChatRoom -> ChatRoomDocument 변환
    fun toDocument(domain: ChatRoom): ChatRoomDocument {
        // 참여자 ID 변환 (String -> ObjectId)
        val participantIds = domain.participants.map(::ObjectId).toSet()

        // 마지막 메시지 ID 변환
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

    // ChatRoomDocument -> ChatRoom 변환
    fun toDomain(document: ChatRoomDocument): ChatRoom {
        return ChatRoom(
            id = document.id?.toString(),
            participants = document.participants.map { it.toString() }.toSet(),
            lastMessageId = document.lastMessageId?.toString(),
            metadata = toMetadata(document.metadata),
            lastActiveAt = document.lastActiveAt,
            createdAt = document.createdAt,
            updatedAt = document.updatedAt
        )
    }

    // ChatRoomMetadata -> ChatRoomMetadataDocument 변환
    private fun toMetadataDocument(metadata: ChatRoomMetadata): ChatRoomMetadataDocument {
        // 참여자 메타데이터의 키를 ObjectId로 변환하고, 값을 ParticipantDocument로 변환
        val participantMetadata = metadata.participantsMetadata.entries.associate { (key, value) ->
            ObjectId(key) to toParticipantDocument(value)
        }

        return ChatRoomMetadataDocument(
            title = metadata.title,
            type = metadata.type,
            participantsMetadata = participantMetadata,
            settings = toSettingsDocument(metadata.settings)
        )
    }

    // ChatRoomMetadataDocument -> ChatRoomMetadata 변환
    private fun toMetadata(document: ChatRoomMetadataDocument): ChatRoomMetadata {
        // 참여자 메타데이터의 키를 String으로 변환하고, 값을 Participant로 변환
        val participantMetadata = document.participantsMetadata.entries.associate { (key, value) ->
            key.toString() to toParticipant(value)
        }

        return ChatRoomMetadata(
            title = document.title,
            type = document.type,
            participantsMetadata = participantMetadata,
            settings = toSettings(document.settings)
        )
    }

    // Participant -> ParticipantDocument 변환
    private fun toParticipantDocument(participant: Participant): ParticipantDocument {
        return ParticipantDocument(
            lastReadMessageId = participant.lastReadMessageId?.let { ObjectId(it) },
            lastReadAt = participant.lastReadAt,
            joinedAt = participant.joinedAt,
            role = participant.role,
            nickname = participant.nickname,
            isActive = participant.isActive
        )
    }

    // ParticipantDocument -> Participant 변환
    private fun toParticipant(document: ParticipantDocument): Participant {
        return Participant(
            lastReadMessageId = document.lastReadMessageId?.toString(),
            lastReadAt = document.lastReadAt,
            joinedAt = document.joinedAt,
            role = document.role,
            nickname = document.nickname,
            isActive = document.isActive
        )
    }

    // ChatRoomSettings -> ChatRoomSettingsDocument 변환
    private fun toSettingsDocument(settings: ChatRoomSettings): ChatRoomSettingsDocument {
        return ChatRoomSettingsDocument(
            isNotificationEnabled = settings.isNotificationEnabled,
            retentionDays = settings.retentionDays,
            isEncrypted = settings.isEncrypted,
            customSettings = settings.customSettings
        )
    }

    // ChatRoomSettingsDocument -> ChatRoomSettings 변환
    private fun toSettings(document: ChatRoomSettingsDocument): ChatRoomSettings {
        return ChatRoomSettings(
            isNotificationEnabled = document.isNotificationEnabled,
            retentionDays = document.retentionDays,
            isEncrypted = document.isEncrypted,
            customSettings = document.customSettings
        )
    }

}