package com.stark.shoot.application.service.message.thread

import com.stark.shoot.adapter.out.persistence.mongodb.mapper.ChatMessageMapper
import com.stark.shoot.application.port.out.message.MessageQueryPort
import com.stark.shoot.application.port.out.message.thread.ThreadQueryPort
import com.stark.shoot.domain.chat.message.ChatMessage
import com.stark.shoot.domain.chat.message.type.MessageStatus
import com.stark.shoot.domain.chat.message.type.MessageType
import com.stark.shoot.domain.chat.message.vo.MessageContent
import com.stark.shoot.domain.chat.message.vo.MessageId
import com.stark.shoot.domain.chatroom.vo.ChatRoomId
import com.stark.shoot.domain.user.vo.UserId
import com.stark.shoot.infrastructure.exception.web.ResourceNotFoundException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.*
import java.time.Instant

@DisplayName("스레드 상세 조회 서비스 테스트")
class GetThreadDetailServiceTest {

    private val messageQueryPort = mock(MessageQueryPort::class.java)
    private val threadQueryPort = mock(ThreadQueryPort::class.java)
    private val chatMessageMapper = ChatMessageMapper()

    private val getThreadDetailService = GetThreadDetailService(
        messageQueryPort,
        threadQueryPort,
        chatMessageMapper
    )

    @Nested
    @DisplayName("스레드 상세 조회 시")
    inner class GetThreadDetail {

        @Test
        @DisplayName("루트 메시지와 스레드 메시지를 함께 조회할 수 있다")
        fun `루트 메시지와 스레드 메시지를 함께 조회할 수 있다`() {
            val threadId = "5f9f1b9b9c9d1b9b9c9d1b9b"
            val rootMessage = ChatMessage(
                id = MessageId.from(threadId),
                roomId = ChatRoomId.from(1L),
                senderId = UserId.from(2L),
                content = MessageContent("root", MessageType.TEXT),
                status = MessageStatus.SAVED,
                createdAt = Instant.now()
            )
            val reply = ChatMessage(
                id = MessageId.from("5f9f1b9b9c9d1b9b9c9d1b9c"),
                roomId = ChatRoomId.from(1L),
                senderId = UserId.from(3L),
                content = MessageContent("reply", MessageType.TEXT),
                status = MessageStatus.SAVED,
                threadId = MessageId.from(threadId),
                createdAt = Instant.now()
            )

            `when`(messageQueryPort.findById(MessageId.from(threadId))).thenReturn(rootMessage)
            `when`(threadQueryPort.findByThreadId(MessageId.from(threadId), 20)).thenReturn(listOf(reply))

            val result = getThreadDetailService.getThreadDetail(MessageId.from(threadId), null, 20)

            assertThat(result.rootMessage.id).isEqualTo(threadId)
            assertThat(result.messages).hasSize(1)
            verify(messageQueryPort).findById(MessageId.from(threadId))
            verify(threadQueryPort).findByThreadId(MessageId.from(threadId), 20)
        }

        @Test
        @DisplayName("존재하지 않는 스레드는 예외를 발생시킨다")
        fun `존재하지 않는 스레드는 예외를 발생시킨다`() {
            val threadId = "5f9f1b9b9c9d1b9b9c9d1b9b"
            `when`(messageQueryPort.findById(MessageId.from(threadId))).thenReturn(null)

            assertThrows<ResourceNotFoundException> {
                getThreadDetailService.getThreadDetail(MessageId.from(threadId), null, 20)
            }

            verify(messageQueryPort).findById(MessageId.from(threadId))
            verify(threadQueryPort, never()).findByThreadId(any(), anyInt())
        }
    }
}
