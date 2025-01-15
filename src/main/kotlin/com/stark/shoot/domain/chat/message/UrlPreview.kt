package com.stark.shoot.domain.chat.message

import java.time.Instant

data class UrlPreview(
    val url: String,
    val title: String?,
    val description: String?,
    val imageUrl: String?,
    val siteName: String? = null,
    val fetchedAt: Instant = Instant.now()
)