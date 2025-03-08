package com.stark.shoot.domain.chat.message

import java.time.Instant

// 채팅 메시지의 임시 저장 정보
data class DraftMessage(
    val id: String? = null,
    val userId: String,                      // 사용자 ID
    val roomId: String,                      // 채팅방 ID
    val content: String,                     // 내용
    val attachments: List<String> = emptyList(), // 첨부파일 ID 목록
    val mentions: Set<String> = emptySet(),  // 멘션된 사용자 ID 목록
    val createdAt: Instant = Instant.now(),  // 생성 시간
    val updatedAt: Instant? = null,          // 수정 시간
    val metadata: MutableMap<String, Any> = mutableMapOf() // 추가 메타데이터
)