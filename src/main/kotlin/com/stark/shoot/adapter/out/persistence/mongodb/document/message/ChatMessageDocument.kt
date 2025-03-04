package com.stark.shoot.adapter.out.persistence.mongodb.document.message

import com.stark.shoot.adapter.out.persistence.mongodb.document.common.BaseMongoDocument
import com.stark.shoot.adapter.out.persistence.mongodb.document.message.embedded.MessageContentDocument
import com.stark.shoot.adapter.out.persistence.mongodb.document.message.embedded.type.MessageStatus
import org.bson.types.ObjectId
import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.index.CompoundIndexes
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

@Document(collection = "messages")
@CompoundIndexes(
    CompoundIndex(name = "room_created_idx", def = "{'roomId': 1, 'createdAt': -1}"),
    CompoundIndex(name = "sender_created_idx", def = "{'senderId': 1, 'createdAt': -1}")
)
data class ChatMessageDocument(
    val roomId: ObjectId,                            // 메시지가 속한 채팅방 ID
    val senderId: ObjectId,                          // 메시지 보낸 사용자 ID
    val content: MessageContentDocument,             // 메시지 내용
    val status: MessageStatus = MessageStatus.SAVED, // 메시지 상태 (e.g., SENT, READ 등)
    val replyToMessageId: ObjectId? = null,          // 답장할 메시지 ID
    val reactions: Map<String, Set<ObjectId>> = emptyMap(), // 이모티콘 ID to 사용자 ID 목록
    val mentions: Set<ObjectId> = emptySet(),        // 멘션된 사용자 ID 목록
    val isDeleted: Boolean = false,                  // 삭제 여부
    val readBy: MutableMap<String, Boolean> = mutableMapOf(), // 읽음 상태 추가
    var metadata: MutableMap<String, Any> = mutableMapOf(),   // 메타데이터 필드 추가

    val isPinned: Boolean = false,  // 고정 여부
    val pinnedBy: ObjectId? = null, // 고정한 사용자 ID
    val pinnedAt: Instant? = null   // 고정 시간
) : BaseMongoDocument()