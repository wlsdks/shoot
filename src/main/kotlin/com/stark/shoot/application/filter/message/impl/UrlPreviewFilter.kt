package com.stark.shoot.application.filter.message.impl

import com.stark.shoot.adapter.out.persistence.mongodb.document.message.embedded.type.MessageType
import com.stark.shoot.application.filter.common.MessageProcessingFilter
import com.stark.shoot.application.filter.message.chain.MessageProcessingChain
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
        // TEXT 타입 메시지만 처리
        if (message.content.type != MessageType.TEXT ||
            message.content.text.length < 8
        ) {  // 최소 "http://" 길이 확인
            return chain.proceed(message)
        }

        // URL 추출 - lazy 처리
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
        if (preview != null) {
            // 현재 메타데이터가 있으면 재사용, 없으면 새로 생성
            val metadata = message.content.metadata ?: MessageMetadata()

            // URL 미리보기 설정
            val updatedMetadata = metadata.copy(urlPreview = preview)
            val updatedContent = message.content.copy(metadata = updatedMetadata)

            // 필요한 경우에만 새 메시지 객체 생성
            val messageWithPreview = message.copy(content = updatedContent)
            return chain.proceed(messageWithPreview)
        }

        // 미리보기 정보가 없으면 원본 메시지 반환
        return chain.proceed(message)
    }

}