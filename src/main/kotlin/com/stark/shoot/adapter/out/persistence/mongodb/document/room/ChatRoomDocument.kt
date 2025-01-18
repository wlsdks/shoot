package com.stark.shoot.adapter.out.persistence.mongodb.document.room

import com.stark.shoot.adapter.out.persistence.mongodb.document.common.BaseMongoDocument
import com.stark.shoot.adapter.out.persistence.mongodb.document.room.embedded.ChatRoomMetadataDocument
import org.bson.types.ObjectId
import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.index.CompoundIndexes
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

@Document(collection = "chat_rooms")
@CompoundIndexes(
    CompoundIndex(
        name = "participants_idx",
        def = "{'participants': 1, 'lastActiveAt': -1}"
    )
)
data class ChatRoomDocument(
    val participants: MutableSet<ObjectId>,       // 채팅방 참여자들의 ID
    val lastMessageId: ObjectId? = null,          // 마지막 메시지 ID
    val metadata: ChatRoomMetadataDocument,       // 채팅방 메타데이터 (임베디드)
    val lastActiveAt: Instant = Instant.now()     // 마지막 활동 시간
) : BaseMongoDocument()