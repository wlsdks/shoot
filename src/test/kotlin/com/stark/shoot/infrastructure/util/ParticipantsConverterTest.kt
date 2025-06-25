package com.stark.shoot.infrastructure.util

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("ParticipantsConverter 테스트")
class ParticipantsConverterTest {

    private val converter = ParticipantsConverter()

    @Test
    @DisplayName("[happy] 리스트를 JSON 문자열로 변환한다")
    fun `리스트를 JSON 문자열로 변환한다`() {
        val json = converter.convertToDatabaseColumn(listOf(1L, 2L))
        assertThat(json.replace(" ", "")).isEqualTo("[1,2]")
    }

    @Test
    @DisplayName("[happy] null 리스트는 빈 배열 문자열을 반환한다")
    fun `null 리스트는 빈 배열 문자열을 반환한다`() {
        val json = converter.convertToDatabaseColumn(null)
        assertThat(json).isEqualTo("[]")
    }

    @Test
    @DisplayName("[happy] JSON 문자열을 리스트로 변환한다")
    fun `JSON 문자열을 리스트로 변환한다`() {
        val list = converter.convertToEntityAttribute("[1,2,3]")
        assertThat(list).containsExactly(1L, 2L, 3L)
    }

    @Test
    @DisplayName("[happy] null 문자열은 빈 리스트를 반환한다")
    fun `null 문자열은 빈 리스트를 반환한다`() {
        val list = converter.convertToEntityAttribute(null)
        assertThat(list).isEmpty()
    }
}
