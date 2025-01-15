package com.stark.shoot.adapter.out.persistence.mongodb.document.message.embedded

import java.time.Instant

data class UrlPreviewDocument(
    val url: String,
    val title: String?,
    val description: String?,
    val imageUrl: String?,
    val siteName: String? = null,
    val fetchedAt: Instant = Instant.now()
)