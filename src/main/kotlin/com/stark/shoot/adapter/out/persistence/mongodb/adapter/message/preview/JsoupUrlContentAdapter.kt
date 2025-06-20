package com.stark.shoot.adapter.out.persistence.mongodb.adapter.message.preview

import com.stark.shoot.application.port.out.message.preview.LoadUrlContentPort
import com.stark.shoot.domain.chat.message.vo.ChatMessageMetadata
import com.stark.shoot.infrastructure.annotation.Adapter
import io.github.oshai.kotlinlogging.KotlinLogging
import org.jsoup.Jsoup
import java.net.URL
import java.time.Instant

@Adapter
class JsoupUrlContentAdapter : LoadUrlContentPort {

    private val logger = KotlinLogging.logger {}

    override fun fetchUrlContent(
        url: String
    ): ChatMessageMetadata.UrlPreview? {
        try {
            // Jsoup으로 URL 내용 파싱 (타임아웃 3초 설정)
            val document = Jsoup.connect(url)
                .userAgent("Mozilla/5.0")
                .timeout(3000)
                .followRedirects(true)
                .get()

            // Open Graph 태그나 일반 메타 태그에서 정보 추출
            val title = document.select("meta[property=og:title]").attr("content")
                ?: document.title()

            val description = document.select("meta[property=og:description]").attr("content")
                ?: document.select("meta[name=description]").attr("content")
                ?: ""

            val imageUrl = document.select("meta[property=og:image]").attr("content")
                ?: document.select("link[rel=image_src]").attr("href")
                ?: ""

            val siteName = document.select("meta[property=og:site_name]").attr("content")
                ?: try {
                    URL(url).host
                } catch (e: Exception) {
                    null
                }

            // 빈 내용인 경우 null 처리
            val normalizedTitle = title.takeIf { it.isNotBlank() }
            val normalizedDesc = description.takeIf { it.isNotBlank() }
            val normalizedImage = imageUrl.takeIf { it.isNotBlank() }

            // 최소한 제목이나 설명 중 하나는 있어야 함
            if (normalizedTitle == null && normalizedDesc == null) {
                logger.warn { "URL에서 추출할 미리보기 내용이 없음: $url" }
                return null
            }

            return ChatMessageMetadata.UrlPreview(
                url = url,
                title = normalizedTitle,
                description = normalizedDesc,
                imageUrl = normalizedImage,
                siteName = siteName,
                fetchedAt = Instant.now()
            )
        } catch (e: Exception) {
            logger.error(e) { "URL 내용 불러오기 실패: $url" }
            return null
        }
    }

}