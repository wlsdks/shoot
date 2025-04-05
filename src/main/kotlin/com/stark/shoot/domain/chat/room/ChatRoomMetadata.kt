package com.stark.shoot.domain.chat.room

import com.stark.shoot.adapter.out.persistence.postgres.entity.ChatRoomType

data class ChatRoomMetadata(
    val title: String? = null,
    val type: ChatRoomType,
    val participantsMetadata: Map<Long, Participant>,
    val settings: ChatRoomSettings,
    val announcement: String? = null // 공지사항
)