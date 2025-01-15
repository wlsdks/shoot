package com.stark.shoot.domain.chat.message

import java.time.Instant

data class MessageMetadata(
    val urlPreview: UrlPreview? = null,
    var readAt: Instant? = null
)