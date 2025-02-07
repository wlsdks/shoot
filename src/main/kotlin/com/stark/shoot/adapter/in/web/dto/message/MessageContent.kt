package com.stark.shoot.adapter.`in`.web.dto.message

data class MessageContentRequest(
    val text: String,
    val type: String,
    val attachments: List<Any> = emptyList(),
    val isEdited: Boolean = false,
    val isDeleted: Boolean = false
) {

}