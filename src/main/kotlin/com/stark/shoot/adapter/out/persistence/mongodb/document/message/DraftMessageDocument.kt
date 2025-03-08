package com.stark.shoot.adapter.out.persistence.mongodb.document.message

import com.stark.shoot.adapter.out.persistence.mongodb.document.common.BaseMongoDocument
import org.bson.types.ObjectId
import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.index.CompoundIndexes
import org.springframework.data.mongodb.core.mapping.Document

@Document(collection = "draft_messages")
@CompoundIndexes(
    CompoundIndex(name = "user_room_unique_idx", def = "{'userId': 1, 'roomId': 1}", unique = true)
)
data class DraftMessageDocument(
    val userId: ObjectId,
    val roomId: ObjectId,
    val content: String,
    val attachments: List<String> = emptyList(),
    val mentions: Set<String> = emptySet(),
    val metadata: MutableMap<String, Any> = mutableMapOf()
) : BaseMongoDocument()