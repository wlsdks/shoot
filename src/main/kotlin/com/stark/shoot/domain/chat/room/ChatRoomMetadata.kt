package com.stark.shoot.domain.chat.room

import com.stark.shoot.adapter.out.persistence.mongodb.document.room.embedded.type.ChatRoomType
import org.bson.types.ObjectId

data class ChatRoomMetadata(
    val title: String? = null,
    val type: ChatRoomType,
    val participantsMetadata: Map<ObjectId, Participant>,
    val settings: ChatRoomSettings
)