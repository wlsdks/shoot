package com.stark.shoot.adapter.out.persistence.mongodb.document.message.reaction

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.index.CompoundIndexes
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

/**
 * 메시지 리액션을 나타내는 MongoDB 문서
 *
 * DDD Aggregate 분리:
 * - ChatMessage와 독립적인 Aggregate로 관리
 * - 높은 동시성 처리 (여러 사용자가 동시에 리액션 추가 가능)
 * - Eventual Consistency 수용
 */
@Document(collection = "message_reactions")
@CompoundIndexes(
    // 메시지별 리액션 조회 (가장 빈번)
    CompoundIndex(name = "message_id_idx", def = "{'messageId': 1, 'createdAt': -1}"),

    // 사용자별 리액션 조회 (메시지당 1개 제약)
    CompoundIndex(name = "message_user_idx", def = "{'messageId': 1, 'userId': 1}", unique = true),

    // 리액션 타입별 조회
    CompoundIndex(name = "message_type_idx", def = "{'messageId': 1, 'reactionType': 1}")
)
data class MessageReactionDocument(
    @Id
    val id: String? = null,

    val messageId: String,              // 메시지 ID (ID reference to ChatMessage)

    val userId: Long,                   // 사용자 ID (ID reference to User)

    val reactionType: String,           // 리액션 타입 (LIKE, LOVE, HAHA, WOW, SAD, ANGRY)

    val createdAt: Instant = Instant.now(),

    val updatedAt: Instant? = null      // 리액션 변경 시간 (타입 변경 시)
)
