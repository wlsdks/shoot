package com.stark.shoot.infrastructure.util

import org.assertj.core.api.Assertions.assertThat
import org.bson.types.ObjectId
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

@DisplayName("toObjectId 확장 함수 테스트")
class ToObjectIdTest {

    @Test
    @DisplayName("[happy] 문자열을 ObjectId 로 변환한다")
    fun `문자열을 ObjectId 로 변환한다`() {
        val id = ObjectId().toHexString()
        val result = id.toObjectId()
        assertThat(result).isEqualTo(ObjectId(id))
    }

    @Test
    @DisplayName("[bad] 잘못된 문자열이면 예외가 발생한다")
    fun `잘못된 문자열이면 예외가 발생한다`() {
        assertThrows<IllegalArgumentException> {
            "invalid".toObjectId()
        }
    }

    @Test
    @DisplayName("[special] default-message-id는 기본 ObjectId를 반환한다")
    fun `default-message-id는 기본 ObjectId를 반환한다`() {
        val result = "default-message-id".toObjectId()
        assertThat(result).isEqualTo(ObjectId("000000000000000000000000"))
    }
}
