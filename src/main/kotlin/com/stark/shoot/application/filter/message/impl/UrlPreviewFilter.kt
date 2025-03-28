package com.stark.shoot.application.filter.message.impl

import com.stark.shoot.adapter.out.persistence.mongodb.document.message.embedded.type.MessageType
import com.stark.shoot.application.filter.message.chain.MessageProcessingChain
import com.stark.shoot.application.filter.common.MessageProcessingFilter
import com.stark.shoot.application.port.out.message.preview.CacheUrlPreviewPort
import com.stark.shoot.application.port.out.message.preview.ExtractUrlPort
import com.stark.shoot.application.port.out.message.preview.LoadUrlContentPort
import com.stark.shoot.domain.chat.message.ChatMessage
import com.stark.shoot.domain.chat.message.MessageMetadata
import org.springframework.stereotype.Component

@Component
class UrlPreviewFilter(
    private val extractUrlPort: ExtractUrlPort,
    private val loadUrlContentPort: LoadUrlContentPort,
    private val cacheUrlPreviewPort: CacheUrlPreviewPort,
) : MessageProcessingFilter {

    override suspend fun process(
        message: ChatMessage,
        chain: MessageProcessingChain
    ): ChatMessage {
        if (message.content.type != MessageType.TEXT) {
            return chain.proceed(message)
        }

        // URL 추출
        val urls = extractUrlPort.extractUrls(message.content.text)
        if (urls.isEmpty()) {
            return chain.proceed(message)
        }

        // 첫 번째 URL만 처리
        val url = urls.first()

        // 캐시된 미리보기 확인
        val preview = cacheUrlPreviewPort.getCachedUrlPreview(url)
            ?: loadUrlContentPort.fetchUrlContent(url)?.also {
                // 캐시에 저장
                cacheUrlPreviewPort.cacheUrlPreview(url, it)
            }

        // 미리보기 정보가 있으면 메시지에 추가
        return if (preview != null) {
            val currentMetadata = message.content.metadata ?: MessageMetadata()
            val updatedMetadata = currentMetadata.copy(urlPreview = preview)
            val updatedContent = message.content.copy(metadata = updatedMetadata)
            val updatedMessage = message.copy(content = updatedContent)

            chain.proceed(updatedMessage)
        } else {
            chain.proceed(message)
        }
    }

}