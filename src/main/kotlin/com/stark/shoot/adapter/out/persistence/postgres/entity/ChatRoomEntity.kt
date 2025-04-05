package com.stark.shoot.adapter.out.persistence.postgres.entity

import com.stark.shoot.infrastructure.util.ParticipantsConverter
import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "chat_rooms")
class ChatRoomEntity(
    title: String?,
    type: ChatRoomType,
    participantIds: List<Long>,
    lastMessageId: Long?,
    metadata: ChatRoomMetadataEntity,
    lastActiveAt: Instant
) : BaseEntity() {

    var title: String? = title
        protected set

    @Enumerated(EnumType.STRING)
    var type: ChatRoomType = type
        protected set

    @Convert(converter = ParticipantsConverter::class)
    @Column(name = "participant_ids", columnDefinition = "jsonb")
    var participantIds: List<Long> = participantIds
        protected set

    var lastMessageId: Long? = lastMessageId
        protected set

    @Embedded
    var metadata: ChatRoomMetadataEntity = metadata
        protected set

    var lastActiveAt: Instant = lastActiveAt
        protected set

}
