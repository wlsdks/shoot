package com.stark.shoot.domain.chat.message.util

/**
 * 텍스트 콘텐츠 XSS 방지 유틸리티
 * HTML 특수 문자를 이스케이프하여 스크립트 인젝션을 방지합니다.
 */
object TextSanitizer {

    /**
     * HTML 특수 문자를 이스케이프합니다.
     *
     * XSS 공격을 방지하기 위해 다음 문자들을 HTML 엔티티로 변환합니다:
     * - < 를 &lt;로
     * - > 를 &gt;로
     * - & 를 &amp;로
     * - " 를 &quot;로
     * - ' 를 &#x27;로
     *
     * @param text 이스케이프할 텍스트
     * @return 이스케이프된 텍스트
     */
    fun sanitize(text: String): String {
        return text
            .replace("&", "&amp;")   // & 를 먼저 처리 (다른 엔티티에 영향 방지)
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#x27;")
    }
}
