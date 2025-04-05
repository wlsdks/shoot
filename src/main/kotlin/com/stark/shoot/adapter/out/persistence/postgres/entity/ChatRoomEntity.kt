package com.stark.shoot.adapter.out.persistence.postgres.entity

import com.stark.shoot.adapter.out.persistence.postgres.entity.enumerate.ChatRoomType
import com.stark.shoot.infrastructure.util.ParticipantsConverter
import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "chat_rooms")
class ChatRoomEntity(
    title: String?,
    type: ChatRoomType,
    settings: ChatRoomSettingsEntity,
    announcement: String?,
    participantIds: List<Long>,
    pinnedParticipantIds: List<Long>,
    lastMessageId: Long?,
    lastActiveAt: Instant
) : BaseEntity() {

    var title: String? = title
        protected set

    @Enumerated(EnumType.STRING)
    var type: ChatRoomType = type
        protected set

    @Embedded
    var settings: ChatRoomSettingsEntity = settings
        protected set

    var announcement: String? = announcement
        protected set

    @Convert(converter = ParticipantsConverter::class)
    @Column(name = "participant_ids", columnDefinition = "jsonb")
    var participantIds: List<Long> = participantIds
        protected set

    @Convert(converter = ParticipantsConverter::class)
    @Column(name = "pinned_participant_ids", columnDefinition = "jsonb")
    var pinnedParticipantIds: List<Long> = pinnedParticipantIds
        protected set

    var lastMessageId: Long? = lastMessageId
        protected set

    var lastActiveAt: Instant = lastActiveAt
        protected set

}
