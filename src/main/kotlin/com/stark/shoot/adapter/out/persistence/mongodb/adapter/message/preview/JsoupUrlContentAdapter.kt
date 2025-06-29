package com.stark.shoot.adapter.out.persistence.mongodb.adapter.message.preview

import com.stark.shoot.application.port.out.message.preview.LoadUrlContentPort
import com.stark.shoot.domain.chat.message.vo.ChatMessageMetadata
import com.stark.shoot.infrastructure.annotation.Adapter
import io.github.oshai.kotlinlogging.KotlinLogging
import org.jsoup.Connection
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.net.URL
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

@Adapter
class JsoupUrlContentAdapter : LoadUrlContentPort {

    private val logger = KotlinLogging.logger {}

    // 최근 실패한 URL을 캐싱하여 반복적인 요청 방지 (TTL 10분)
    private val failedUrlCache = ConcurrentHashMap<String, Long>()

    companion object {
        // 설정값을 상수로 정의하여 관리 용이성 향상
        private const val TIMEOUT_MS = 5000
        private const val USER_AGENT = "Mozilla/5.0 (compatible; ShootChatBot/1.0; +https://shoot.com/bot)"
        private const val MAX_BODY_SIZE = 1024 * 1024 // 1MB
        private const val FAILED_URL_TTL_MS = 10 * 60 * 1000L // 10분

        // 자주 사용되는 CSS 선택자를 상수로 정의
        private const val OG_TITLE_SELECTOR = "meta[property=og:title]"
        private const val OG_DESC_SELECTOR = "meta[property=og:description]"
        private const val META_DESC_SELECTOR = "meta[name=description]"
        private const val OG_IMAGE_SELECTOR = "meta[property=og:image]"
        private const val IMAGE_SRC_SELECTOR = "link[rel=image_src]"
        private const val OG_SITE_NAME_SELECTOR = "meta[property=og:site_name]"
    }

    override fun fetchUrlContent(url: String): ChatMessageMetadata.UrlPreview? {
        if (url.isBlank()) {
            logger.warn { "빈 URL이 제공되었습니다" }
            return null
        }

        // 최근 실패한 URL인지 확인
        val failedTime = failedUrlCache[url]
        if (failedTime != null && System.currentTimeMillis() - failedTime < FAILED_URL_TTL_MS) {
            logger.debug { "최근 실패한 URL 요청 무시: $url" }
            return null
        }

        try {
            // Jsoup 연결 설정 최적화
            val connection = createOptimizedConnection(url)
            val document = executeWithTimeout(connection)

            // 문서가 null이면 처리 중단
            if (document == null) {
                cacheFailedUrl(url)
                return null
            }

            // 메타데이터 추출 및 정규화
            val preview = extractAndNormalizeMetadata(document, url)

            // 유효한 미리보기 데이터가 있는지 확인
            if (preview == null) {
                cacheFailedUrl(url)
            }

            return preview
        } catch (e: Exception) {
            logger.error(e) { "URL 내용 불러오기 실패: $url - ${e.message}" }
            cacheFailedUrl(url)
            return null
        }
    }

    /**
     * 최적화된 Jsoup 연결 객체 생성
     */
    private fun createOptimizedConnection(url: String): Connection {
        return Jsoup.connect(url)
            .userAgent(USER_AGENT)
            .timeout(TIMEOUT_MS)
            .followRedirects(true)
            .ignoreContentType(true)
            .ignoreHttpErrors(true)
            .maxBodySize(MAX_BODY_SIZE)
            .header("Accept", "text/html,application/xhtml+xml,application/xml")
            .header("Accept-Language", "ko-KR,ko;q=0.9,en-US;q=0.8,en;q=0.7")
            .method(Connection.Method.GET)
        // SSL 인증서 검증은 Jsoup 기본 설정 사용
    }

    /**
     * 타임아웃 처리가 개선된 연결 실행
     */
    private fun executeWithTimeout(connection: Connection): Document? {
        return try {
            connection.get()
        } catch (e: Exception) {
            logger.warn { "URL 연결 실패: ${connection.request().url()} - ${e.message}" }
            null
        }
    }

    /**
     * 문서에서 메타데이터 추출 및 정규화
     */
    private fun extractAndNormalizeMetadata(
        document: Document,
        url: String
    ): ChatMessageMetadata.UrlPreview? {
        // Open Graph 태그나 일반 메타 태그에서 정보 추출
        val title = document.select(OG_TITLE_SELECTOR).attr("content")
            ?: document.title()

        val description = document.select(OG_DESC_SELECTOR).attr("content")
            ?: document.select(META_DESC_SELECTOR).attr("content")
            ?: ""

        val imageUrl = document.select(OG_IMAGE_SELECTOR).attr("content")
            ?: document.select(IMAGE_SRC_SELECTOR).attr("href")
            ?: ""

        val siteName = document.select(OG_SITE_NAME_SELECTOR).attr("content")
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
    }

    /**
     * 실패한 URL을 캐시에 추가
     */
    private fun cacheFailedUrl(url: String) {
        failedUrlCache[url] = System.currentTimeMillis()

        // 캐시 크기 관리 (100개 이상이면 오래된 항목 제거)
        if (failedUrlCache.size > 100) {
            val currentTime = System.currentTimeMillis()
            failedUrlCache.entries.removeIf {
                currentTime - it.value > FAILED_URL_TTL_MS
            }
        }
    }
}
