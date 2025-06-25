package com.stark.shoot.domain.chatroom.vo

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("채팅방 VO 테스트")
class ChatRoomValueObjectsTest {

    @Nested
    inner class ChatRoomTitleTest {
        @Test
        @DisplayName("[happy] 유효한 값으로 생성")
        fun `유효한 값으로 생성`() {
            val title = ChatRoomTitle.from("title")
            assertThat(title.value).isEqualTo("title")
        }

        @Test
        @DisplayName("[bad] 빈 값은 예외")
        fun `빈 값은 예외`() {
            assertThatThrownBy { ChatRoomTitle.from(" ") }
                .isInstanceOf(IllegalArgumentException::class.java)
        }
    }

    @Nested
    inner class ChatRoomAnnouncementTest {
        @Test
        @DisplayName("[happy] 유효한 값으로 생성")
        fun `유효한 값으로 생성`() {
            val ann = ChatRoomAnnouncement.from("hello")
            assertThat(ann.value).isEqualTo("hello")
        }

        @Test
        @DisplayName("[bad] 빈 값은 예외")
        fun `빈 값은 예외`() {
            assertThatThrownBy { ChatRoomAnnouncement.from("") }
                .isInstanceOf(IllegalArgumentException::class.java)
        }

        @Test
        @DisplayName("[bad] 200자 초과시 예외")
        fun `200자 초과시 예외`() {
            val long = "a".repeat(201)
            assertThatThrownBy { ChatRoomAnnouncement.from(long) }
                .isInstanceOf(IllegalArgumentException::class.java)
        }
    }

    @Nested
    inner class ChatRoomIdTest {
        @Test
        @DisplayName("[happy] 양수 값으로 생성")
        fun `양수 값으로 생성`() {
            val id = ChatRoomId.from(1L)
            assertThat(id.value).isEqualTo(1L)
        }

        @Test
        @DisplayName("[bad] 0이하 값은 예외")
        fun `0이하 값은 예외`() {
            assertThatThrownBy { ChatRoomId.from(0) }
                .isInstanceOf(IllegalArgumentException::class.java)
        }
    }

    @Nested
    inner class RetentionDaysTest {
        @Test
        @DisplayName("[happy] 양수 값으로 생성")
        fun `양수 값으로 생성`() {
            val d = RetentionDays.from(5)
            assertThat(d.value).isEqualTo(5)
        }

        @Test
        @DisplayName("[bad] 0이하 값은 예외")
        fun `0이하 값은 예외`() {
            assertThatThrownBy { RetentionDays.from(0) }
                .isInstanceOf(IllegalArgumentException::class.java)
        }
    }
}
