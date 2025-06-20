package com.stark.shoot.adapter.`in`.web.dto.message

import com.stark.shoot.domain.chat.message.vo.ChatMessageMetadata
import java.time.Instant

data class ChatMessageMetadataRequest(
    val tempId: String? = null,
    var needsUrlPreview: Boolean = false,
    var previewUrl: String? = null,
    var urlPreview: ChatMessageMetadata.UrlPreview? = null,
    var readAt: Instant? = null,
//    var scheduledMessageId: Long? = null
)