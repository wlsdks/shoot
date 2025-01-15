package com.stark.shoot.application.port.out

import org.bson.types.ObjectId
import java.time.Instant

interface LoadChatMessagePort {

    fun findById(id: ObjectId): ChatMessage?
    fun findByRoomId(roomId: ObjectId): List<ChatMessage>
    fun findByRoomIdAndBeforeCreatedAt(roomId: ObjectId, createdAt: Instant): List<ChatMessage>

}