package com.stark.shoot.application.port.out.message.preview

import com.stark.shoot.domain.chat.message.UrlPreview

interface LoadUrlContentPort {
    fun fetchUrlContent(url: String): UrlPreview?
}