package com.stark.shoot.adapter.out.persistence.mongodb.adapter.message.preview

import com.stark.shoot.application.port.out.message.preview.ExtractUrlPort
import com.stark.shoot.infrastructure.annotation.Adapter
import io.github.oshai.kotlinlogging.KotlinLogging

@Adapter
class RegexUrlExtractorAdapter : ExtractUrlPort {

    private val logger = KotlinLogging.logger {}

    /**
     * 텍스트에서 URL 추출
     * @param text URL을 추출할 텍스트
     * @return 추출된 URL 리스트
     */
    override fun extractUrls(
        text: String
    ): List<String> {
        if (text.isBlank()) {
            return emptyList()
        }

        try {
            return URL_REGEX.findAll(text).map { it.value }.toList()
        } catch (e: Exception) {
            logger.error(e) { "URL 추출 중 오류 발생: ${e.message}" }
            return emptyList()
        }
    }

    companion object {
        // 정규식 패턴을 컴파일하여 재사용 (성능 최적화)
        private val URL_REGEX = Regex(
            "https?://[-a-zA-Z0-9@:%._+~#=]{1,256}\\.[a-zA-Z0-9()]{1,6}\\b([-a-zA-Z0-9()@:%_+.~#?&/=]*)"
        )
    }
}
