package com.stark.shoot.domain.chat.bookmark

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.Instant

@DisplayName("메시지 북마크 도메인 테스트")
class MessageBookmarkTest {

    @Nested
    @DisplayName("북마크 생성 시")
    inner class CreateBookmark {
        @Test
        fun `필수 정보로 북마크를 생성할 수 있다`() {
            val bookmark = MessageBookmark(messageId = "m1", userId = 1L)

            assertThat(bookmark.id).isNull()
            assertThat(bookmark.messageId).isEqualTo("m1")
            assertThat(bookmark.userId).isEqualTo(1L)
            assertThat(bookmark.createdAt).isNotNull()
        }

        @Test
        fun `모든 정보를 사용하여 북마크를 생성할 수 있다`() {
            val now = Instant.now()
            val bookmark = MessageBookmark(
                id = "b1",
                messageId = "m1",
                userId = 2L,
                createdAt = now
            )

            assertThat(bookmark.id).isEqualTo("b1")
            assertThat(bookmark.messageId).isEqualTo("m1")
            assertThat(bookmark.userId).isEqualTo(2L)
            assertThat(bookmark.createdAt).isEqualTo(now)
        }
    }
}
