package com.stark.shoot.adapter.`in`.web.dto.message.draft

import java.time.Instant

data class DraftMessageResponseDto(
    val id: String? = null,
    val userId: Long,                      // 사용자 ID
    val roomId: Long,                      // 채팅방 ID
    val content: String,                     // 내용
    val attachments: List<String> = emptyList(), // 첨부파일 ID 목록
    val mentions: Set<String> = emptySet(),  // 멘션된 사용자 ID 목록
    val createdAt: Instant = Instant.now(),  // 생성 시간
    val updatedAt: Instant? = null,          // 수정 시간
    val metadata: MutableMap<String, Any> = mutableMapOf() // 추가 메타데이터
)