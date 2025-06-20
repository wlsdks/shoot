package com.stark.shoot.application.port.out.message.preview

import com.stark.shoot.domain.chat.message.vo.ChatMessageMetadata

interface LoadUrlContentPort {
    fun fetchUrlContent(url: String): ChatMessageMetadata.UrlPreview?
}