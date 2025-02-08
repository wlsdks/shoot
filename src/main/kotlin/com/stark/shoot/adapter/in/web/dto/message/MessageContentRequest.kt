package com.stark.shoot.adapter.`in`.web.dto.message

data class MessageContentRequest(
    val text: String,                         // 메시지 내용
    val type: String,                         // 메시지 타입
    val attachments: List<Any> = emptyList(), // 첨부파일
    val isEdited: Boolean = false,            // 수정 여부
    val isDeleted: Boolean = false            // 삭제 여부
) {
}