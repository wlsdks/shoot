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
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.*
import java.time.Instant

@DisplayName("스레드 상세 조회 서비스 테스트")
class GetThreadDetailServiceTest {

    private lateinit var messageQueryPort: MessageQueryPort
    private lateinit var threadQueryPort: ThreadQueryPort
    private lateinit var chatMessageMapper: ChatMessageMapper
    private lateinit var getThreadDetailService: GetThreadDetailService

    @BeforeEach
    fun setUp() {
        messageQueryPort = mock(MessageQueryPort::class.java)
        threadQueryPort = mock(ThreadQueryPort::class.java)
        chatMessageMapper = ChatMessageMapper()
        getThreadDetailService = GetThreadDetailService(
            messageQueryPort,
            threadQueryPort,
            chatMessageMapper
        )
    }

    @Nested
    @DisplayName("스레드 상세 조회 시")
    inner class GetThreadDetail {

        @Test
        @DisplayName("루트 메시지와 스레드 메시지를 함께 조회할 수 있다")
        fun `루트 메시지와 스레드 메시지를 함께 조회할 수 있다`() {
            // given
            val threadId = "5f9f1b9b9c9d1b9b9c9d1b9b"
            val messageIdObj = MessageId.from(threadId)
            val rootMessage = ChatMessage(
                id = messageIdObj,
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
                threadId = messageIdObj,
                createdAt = Instant.now()
            )

            doReturn(rootMessage).`when`(messageQueryPort).findById(messageIdObj)
            doReturn(listOf(reply)).`when`(threadQueryPort).findByThreadId(messageIdObj, 20)

            // when
            val result = getThreadDetailService.getThreadDetail(messageIdObj, null, 20)

            // then
            assertThat(result.rootMessage.id).isEqualTo(threadId)
            assertThat(result.messages).hasSize(1)
            verify(messageQueryPort).findById(messageIdObj)
            verify(threadQueryPort).findByThreadId(messageIdObj, 20)
        }

        @Test
        @DisplayName("존재하지 않는 스레드는 예외를 발생시킨다")
        fun `존재하지 않는 스레드는 예외를 발생시킨다`() {
            // given
            val threadId = "5f9f1b9b9c9d1b9b9c9d1b9b"
            val messageIdObj = MessageId.from(threadId)
            doReturn(null).`when`(messageQueryPort).findById(messageIdObj)

            // when & then
            assertThrows<ResourceNotFoundException> {
                getThreadDetailService.getThreadDetail(messageIdObj, null, 20)
            }

            verify(messageQueryPort).findById(messageIdObj)
            verifyNoMoreInteractions(messageQueryPort)
            verifyNoInteractions(threadQueryPort)
        }
    }
}
