package com.stark.shoot.domain.chat.message

/**
 * 메시지 편집 가능 여부 결과
 *
 * DDD 개선: ChatRoom Context → Chat Context로 이동
 * - 메시지 편집은 Chat Context의 관심사
 * - Context 독립성 향상
 */
data class EditabilityResult(
    val canEdit: Boolean,
    val reason: String?
)