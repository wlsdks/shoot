package com.stark.shoot.domain.service.chatroom

/**
 * 메시지 편집 가능 여부 결과
 */
data class EditabilityResult(
    val canEdit: Boolean,
    val reason: String?
)