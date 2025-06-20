package com.stark.shoot.application.port.out.message.preview

import com.stark.shoot.domain.chat.message.vo.ChatMessageMetadata

interface CacheUrlPreviewPort {
    fun cacheUrlPreview(url: String, preview: ChatMessageMetadata.UrlPreview)
    fun getCachedUrlPreview(url: String): ChatMessageMetadata.UrlPreview?
}