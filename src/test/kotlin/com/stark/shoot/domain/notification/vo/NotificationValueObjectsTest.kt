package com.stark.shoot.domain.notification.vo

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("알림 VO 테스트")
class NotificationValueObjectsTest {

    @Nested
    inner class NotificationIdTest {
        @Test
        @DisplayName("[happy] 정상 생성")
        fun `정상 생성`() {
            val id = NotificationId.from("id")
            assertThat(id.value).isEqualTo("id")
        }

        @Test
        @DisplayName("[bad] blank 는 예외")
        fun `blank 는 예외`() {
            assertThatThrownBy { NotificationId.from("") }
                .isInstanceOf(IllegalArgumentException::class.java)
        }
    }

    @Nested
    inner class NotificationTitleTest {
        @Test
        @DisplayName("[happy] 정상 생성")
        fun `정상 생성`() {
            val t = NotificationTitle.from("t")
            assertThat(t.value).isEqualTo("t")
        }

        @Test
        @DisplayName("[bad] blank 는 예외")
        fun `blank 는 예외`() {
            assertThatThrownBy { NotificationTitle.from(" ") }
                .isInstanceOf(IllegalArgumentException::class.java)
        }
    }

    @Nested
    inner class NotificationMessageTest {
        @Test
        @DisplayName("[happy] 정상 생성")
        fun `정상 생성`() {
            val m = NotificationMessage.from("m")
            assertThat(m.value).isEqualTo("m")
        }

        @Test
        @DisplayName("[bad] blank 는 예외")
        fun `blank 는 예외`() {
            assertThatThrownBy { NotificationMessage.from("") }
                .isInstanceOf(IllegalArgumentException::class.java)
        }
    }
}
