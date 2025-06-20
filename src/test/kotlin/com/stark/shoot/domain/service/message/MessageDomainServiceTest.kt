package com.stark.shoot.domain.service.message

import com.stark.shoot.domain.chat.event.EventType
import com.stark.shoot.domain.chat.message.service.MessageDomainService
import com.stark.shoot.domain.chat.message.type.MessageType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("메시지 도메인 서비스 테스트")
class MessageDomainServiceTest {

    private val service = MessageDomainService()

    @Test
    fun `메시지를 생성하고 URL 미리보기를 적용할 수 있다`() {
        val result = service.createAndProcessMessage(
            roomId = 1L,
            senderId = 2L,
            contentText = "check https://example.com",
            contentType = MessageType.TEXT,
            extractUrls = { listOf("https://example.com") },
            getCachedPreview = { UrlPreview(it, "Example", "desc", "") }
        )

        assertThat(result.content.text).contains("check")
        assertThat(result.metadata.urlPreview).isNotNull()
        assertThat(result.metadata.urlPreview?.title).isEqualTo("Example")
    }

    @Test
    fun `메시지로 이벤트를 생성할 수 있다`() {
        val message = service.createAndProcessMessage(
            roomId = 1L,
            senderId = 2L,
            contentText = "hello",
            contentType = MessageType.TEXT,
            extractUrls = { emptyList() },
            getCachedPreview = { null }
        )

        val event = service.createMessageEvent(message)

        assertThat(event.type).isEqualTo(EventType.MESSAGE_CREATED)
        assertThat(event.data).isEqualTo(message)
    }
}
