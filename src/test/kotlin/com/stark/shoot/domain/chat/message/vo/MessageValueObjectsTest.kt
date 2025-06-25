package com.stark.shoot.domain.chat.message.vo

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("메시지 VO 테스트")
class MessageValueObjectsTest {

    @Nested
    inner class MessageIdTest {
        @Test
        @DisplayName("[happy] 정상 생성")
        fun `정상 생성`() {
            val id = MessageId.from("id1")
            assertThat(id.value).isEqualTo("id1")
        }

        @Test
        @DisplayName("[bad] blank 는 예외")
        fun `blank 는 예외`() {
            assertThatThrownBy { MessageId.from(" ") }
                .isInstanceOf(IllegalArgumentException::class.java)
        }
    }
}
