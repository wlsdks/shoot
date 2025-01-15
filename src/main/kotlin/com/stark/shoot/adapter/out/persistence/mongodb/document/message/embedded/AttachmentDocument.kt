package com.stark.shoot.adapter.out.persistence.mongodb.document.message.embedded

data class AttachmentDocument(
    val id: String, // 파일의 고유 식별자
    val filename: String,
    val contentType: String,
    val size: Long,
    val url: String,
    val thumbnailUrl: String? = null,
    val metadata: Map<String, Any> = emptyMap() // 추가 메타데이터 (이미지 크기, 길이 등)
)