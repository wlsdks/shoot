package com.stark.shoot.application.port.out

import org.bson.types.ObjectId

interface SaveChatRoomPort {

    fun save(chatRoom: ChatRoom): ChatRoom
    fun updateLastMessage(roomId: ObjectId, lastMessageId: ObjectId)
    fun updateParticipantMetadata(roomId: ObjectId, participantId: ObjectId, lastReadMessageId: ObjectId)

}