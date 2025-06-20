package com.stark.shoot.application.service.message.thread

import com.stark.shoot.adapter.out.persistence.mongodb.mapper.ChatMessageMapper
import com.stark.shoot.application.port.out.message.LoadMessagePort
import com.stark.shoot.domain.chat.message.ChatMessage
import com.stark.shoot.domain.chat.message.vo.MessageContent
import com.stark.shoot.domain.chat.message.type.MessageStatus
import com.stark.shoot.domain.chat.message.type.MessageType
import com.stark.shoot.domain.chat.message.vo.MessageId
import com.stark.shoot.infrastructure.util.toObjectId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import java.time.Instant

@DisplayName("스레드 메시지 조회 서비스 테스트")
class GetThreadMessagesServiceTest {

    private val loadMessagePort = mock(LoadMessagePort::class.java)
    private val chatMessageMapper = ChatMessageMapper()

    private val getThreadMessagesService = GetThreadMessagesService(
        loadMessagePort,
        chatMessageMapper
    )

    @Nested
    @DisplayName("스레드 메시지 조회 시")
    inner class GetThreadMessages {

        @Test
        @DisplayName("스레드의 메시지를 조회할 수 있다")
        fun `스레드의 메시지를 조회할 수 있다`() {
            // given
            val threadId = "5f9f1b9b9c9d1b9b9c9d1b9b"
            val message = ChatMessage(
                id = MessageId.from("5f9f1b9b9c9d1b9b9c9d1b9c"),
                roomId = 1L,
                senderId = 2L,
                content = MessageContent("hello", MessageType.TEXT),
                status = MessageStatus.SAVED,
                threadId = MessageId.from(threadId),
                createdAt = Instant.now()
            )

            `when`(loadMessagePort.findByThreadId(threadId.toObjectId(), 20)).thenReturn(listOf(message))

            // when
            val result = getThreadMessagesService.getThreadMessages(threadId, null, 20)

            // then
            assertThat(result).hasSize(1)
            assertThat(result[0].id).isEqualTo(message.id)

            verify(loadMessagePort).findByThreadId(threadId.toObjectId(), 20)
        }

        @Test
        @DisplayName("스레드 메시지가 없는 경우 빈 목록을 반환한다")
        fun `스레드 메시지가 없는 경우 빈 목록을 반환한다`() {
            val threadId = "5f9f1b9b9c9d1b9b9c9d1b9b"
            `when`(loadMessagePort.findByThreadId(threadId.toObjectId(), 20)).thenReturn(emptyList())

            val result = getThreadMessagesService.getThreadMessages(threadId, null, 20)

            assertThat(result).isEmpty()

            verify(loadMessagePort).findByThreadId(threadId.toObjectId(), 20)
        }
    }
}
