package com.stark.shoot.adapter.out.persistence.mongodb.document.room.embedded

import com.stark.shoot.adapter.out.persistence.mongodb.document.room.embedded.type.ChatRoomType
import org.bson.types.ObjectId

data class ChatRoomMetadataDocument(
    val title: String? = null,                    // 채팅방 제목 (1:1 채팅의 경우 null)
    val type: ChatRoomType = ChatRoomType.INDIVIDUAL,   // 채팅방 타입
    val participantsMetadata: Map<ObjectId, ParticipantDocument> = emptyMap(), // 참여자별 메타데이터
    val settings: ChatRoomSettingsDocument = ChatRoomSettingsDocument()        // 채팅방 설정
)