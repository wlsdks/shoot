package com.stark.shoot.adapter.out.persistence.mongodb.document.message

import com.stark.shoot.adapter.out.persistence.mongodb.document.common.BaseMongoDocument
import com.stark.shoot.adapter.out.persistence.mongodb.document.message.embedded.MessageContentDocument
import com.stark.shoot.adapter.out.persistence.mongodb.document.message.embedded.MessageMetadataDocument
import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.index.CompoundIndexes
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

/**
 * 예약된 메시지를 나타내는 MongoDB 문서
 */
@Document(collection = "scheduled_messages")
@CompoundIndexes(
    CompoundIndex(name = "sender_room_idx", def = "{'senderId': 1, 'roomId': 1, 'scheduledAt': 1}"),
    CompoundIndex(name = "status_scheduled_idx", def = "{'status': 1, 'scheduledAt': 1}")
)
data class ScheduledMessageDocument(
    val roomId: Long,
    val senderId: Long,
    val content: MessageContentDocument,
    val scheduledAt: Instant,
    val status: String, // ScheduledMessageStatus enum 문자열 표현
    val metadata: MessageMetadataDocument = MessageMetadataDocument(),
) : BaseMongoDocument()