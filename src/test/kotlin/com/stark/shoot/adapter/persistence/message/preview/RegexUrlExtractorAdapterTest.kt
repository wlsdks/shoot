package com.stark.shoot.adapter.persistence.message.preview

import com.stark.shoot.adapter.out.persistence.mongodb.adapter.message.preview.RegexUrlExtractorAdapter
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("URL 추출 어댑터(Regex) 테스트")
class RegexUrlExtractorAdapterTest {

    private val adapter = RegexUrlExtractorAdapter()

    @Nested
    @DisplayName("extractUrls")
    inner class ExtractUrls {

        @Test
        @DisplayName("[happy] 텍스트에서 단일 URL을 추출한다")
        fun `텍스트에서 단일 URL을 추출한다`() {
            val result = adapter.extractUrls("Visit https://example.com for info")
            assertThat(result).containsExactly("https://example.com")
        }

        @Test
        @DisplayName("[happy] 텍스트에서 여러 URL을 추출한다")
        fun `텍스트에서 여러 URL을 추출한다`() {
            val text = "Links: https://a.com https://b.org/path"
            val result = adapter.extractUrls(text)
            assertThat(result).containsExactly("https://a.com", "https://b.org/path")
        }

        @Test
        @DisplayName("[happy] URL이 없으면 빈 리스트를 반환한다")
        fun `URL이 없으면 빈 리스트를 반환한다`() {
            val result = adapter.extractUrls("no url here")
            assertThat(result).isEmpty()
        }
    }
}
