package com.stark.shoot.domain.chat.room

import com.stark.shoot.adapter.out.persistence.mongodb.document.room.embedded.type.ChatRoomType

data class ChatRoomMetadata(
    val title: String? = null,
    val type: ChatRoomType,
    val participantsMetadata: Map<String, Participant>,
    val settings: ChatRoomSettings
)