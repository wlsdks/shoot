package com.stark.shoot.application.port.out.message.preview

import com.stark.shoot.domain.chat.message.UrlPreview

interface CacheUrlPreviewPort {
    fun cacheUrlPreview(url: String, preview: UrlPreview)
    fun getCachedUrlPreview(url: String): UrlPreview?
}