package com.stark.shoot.application.port.out

import org.bson.types.ObjectId

interface LoadChatRoomPort {

    fun findById(id: ObjectId): ChatRoom?
    fun findByParticipantId(participantId: ObjectId): List<ChatRoom>
    fun existsByParticipants(participants: Set<ObjectId>): Boolean

}