package com.stark.shoot.domain.chat.message.util

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("텍스트 새니타이저 테스트")
class TextSanitizerTest {

    @Test
    @DisplayName("[happy] HTML 특수문자를 이스케이프한다")
    fun `HTML 특수문자를 이스케이프한다`() {
        val input = "<script>alert('XSS')</script>"
        val result = TextSanitizer.sanitize(input)

        assertThat(result).isEqualTo("&lt;script&gt;alert(&#x27;XSS&#x27;)&lt;/script&gt;")
        assertThat(result).doesNotContain("<script>")
    }

    @Test
    @DisplayName("[happy] 앰퍼샌드를 먼저 이스케이프한다")
    fun `앰퍼샌드를 먼저 이스케이프한다`() {
        val input = "A & B"
        val result = TextSanitizer.sanitize(input)

        assertThat(result).isEqualTo("A &amp; B")
    }

    @Test
    @DisplayName("[happy] 따옴표를 이스케이프한다")
    fun `따옴표를 이스케이프한다`() {
        val input = "He said \"Hello\" and 'Hi'"
        val result = TextSanitizer.sanitize(input)

        assertThat(result).isEqualTo("He said &quot;Hello&quot; and &#x27;Hi&#x27;")
    }

    @Test
    @DisplayName("[happy] 정상적인 텍스트는 변경되지 않는다")
    fun `정상적인 텍스트는 변경되지 않는다`() {
        val input = "Hello World! 안녕하세요 123"
        val result = TextSanitizer.sanitize(input)

        assertThat(result).isEqualTo(input)
    }

    @Test
    @DisplayName("[happy] 혼합된 특수문자를 모두 이스케이프한다")
    fun `혼합된 특수문자를 모두 이스케이프한다`() {
        val input = "<div onclick=\"alert('XSS')\" style=\"color: red;\">Click me & you'll see</div>"
        val result = TextSanitizer.sanitize(input)

        assertThat(result).contains("&lt;div")
        assertThat(result).contains("&gt;")
        assertThat(result).contains("&quot;")
        assertThat(result).contains("&#x27;")
        assertThat(result).contains("&amp;")
        assertThat(result).doesNotContain("<div")
        assertThat(result).doesNotContain("<script")
    }
}
