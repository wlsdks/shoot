package com.stark.shoot.adapter.out.persistence.mongodb.document.message

import com.stark.shoot.adapter.out.persistence.mongodb.document.common.BaseMongoDocument
import com.stark.shoot.adapter.out.persistence.mongodb.document.message.embedded.MessageContentDocument
import com.stark.shoot.domain.chat.message.type.MessageStatus
import org.bson.types.ObjectId
import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.index.CompoundIndexes
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

@Document(collection = "messages")
@CompoundIndexes(
    // 기본 조회 인덱스
    CompoundIndex(name = "room_created_idx", def = "{'roomId': 1, 'createdAt': -1}"),
    CompoundIndex(name = "sender_created_idx", def = "{'senderId': 1, 'createdAt': -1}"),

    // 스레드 메시지 조회 최적화
    CompoundIndex(name = "thread_id_idx", def = "{'threadId': 1, 'createdAt': -1}"),

    // 고정 메시지 조회는 별도 MessagePin collection에서 수행

    // 삭제되지 않은 메시지 조회 최적화
    CompoundIndex(name = "room_not_deleted_idx", def = "{'roomId': 1, 'isDeleted': 1, 'createdAt': -1}")
)
data class ChatMessageDocument(
    val roomId: Long,                                       // 메시지가 속한 채팅방 ID
    val senderId: Long,                                     // 메시지 보낸 사용자 ID
    val content: MessageContentDocument,                    // 메시지 내용
    val status: MessageStatus = MessageStatus.SENT,         // 메시지 상태 (e.g., SENT, READ 등)
    val threadId: ObjectId? = null,                         // 스레드 ID (루트 메시지 ID)
    val replyToMessageId: ObjectId? = null,                 // 답장할 메시지 ID
    val mentions: Set<Long> = emptySet(),                   // 멘션된 사용자 ID 목록
    // 메시지 고정 정보는 별도 MessagePin Aggregate로 분리되었습니다.
    // 메시지 읽음 표시는 별도 MessageReadReceipt Aggregate로 분리되었습니다.
    // 메시지 리액션 정보는 별도 MessageReaction Aggregate로 분리되었습니다.
    val isDeleted: Boolean = false,                         // 삭제 여부
) : BaseMongoDocument()