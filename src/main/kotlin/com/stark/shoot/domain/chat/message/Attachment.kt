package com.stark.shoot.domain.chat.message

data class Attachment(
    val id: String,
    val filename: String,
    val contentType: String,
    val size: Long,
    val url: String,
    val thumbnailUrl: String? = null,
    val metadata: Map<String, Any> = emptyMap()
)
