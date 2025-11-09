package com.stark.shoot.domain.shared.event.type

enum class EventType(
    private val code: String,
    private val description: String
) {
    MESSAGE_CREATED("MESSAGE_CREATED", "메시지 생성"),
    MESSAGE_UPDATED("MESSAGE_UPDATED", "메시지 수정"),
    MESSAGE_DELETED("MESSAGE_DELETED", "메시지 삭제"),
}