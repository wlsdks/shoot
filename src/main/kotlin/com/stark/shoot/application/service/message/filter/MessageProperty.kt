package com.stark.shoot.application.service.message.filter

import com.stark.shoot.domain.chat.room.Participant
import org.bson.types.ObjectId

// 메시지 프로퍼티 (필터 간 데이터 전달용)
data class MessageProperty(
    val updatedParticipants: Map<ObjectId, Participant>
)