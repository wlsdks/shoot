package com.stark.shoot.adapter.out.persistence.mongodb.document.message.embedded

import java.time.Instant

data class MessageMetadataDocument(
    val urlPreview: UrlPreviewDocument? = null,
    val readAt: Instant? = null
)