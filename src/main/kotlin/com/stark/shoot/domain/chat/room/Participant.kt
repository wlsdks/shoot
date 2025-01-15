package com.stark.shoot.domain.chat.room

import com.stark.shoot.adapter.out.persistence.mongodb.document.room.embedded.type.ParticipantRole
import java.time.Instant

data class Participant(
    var lastReadMessageId: String? = null,
    var lastReadAt: Instant? = null,
    val joinedAt: Instant = Instant.now(),
    val role: ParticipantRole = ParticipantRole.MEMBER,
    val nickname: String? = null,
    val isActive: Boolean = true
)