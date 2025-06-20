package com.stark.shoot.application.service.message.thread

import com.stark.shoot.adapter.`in`.web.dto.message.ChatMessageRequest
import com.stark.shoot.adapter.`in`.web.dto.message.MessageContentRequest
import com.stark.shoot.application.port.`in`.message.SendMessageUseCase
import com.stark.shoot.application.port.out.message.LoadMessagePort
import com.stark.shoot.domain.chat.message.ChatMessage
import com.stark.shoot.domain.chat.message.MessageContent
import com.stark.shoot.domain.chat.message.type.MessageStatus
import com.stark.shoot.domain.chat.message.type.MessageType
import com.stark.shoot.domain.common.vo.MessageId
import com.stark.shoot.infrastructure.exception.web.ResourceNotFoundException
import com.stark.shoot.infrastructure.util.toObjectId
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.*
import java.time.Instant

@DisplayName("스레드 메시지 전송 서비스 테스트")
class SendThreadMessageServiceTest {

    private val loadMessagePort = mock(LoadMessagePort::class.java)
    private val sendMessageUseCase = mock(SendMessageUseCase::class.java)

    private val sendThreadMessageService = SendThreadMessageService(
        loadMessagePort,
        sendMessageUseCase
    )

    @Test
    @DisplayName("존재하지 않는 스레드에 메시지를 보내면 예외가 발생한다")
    fun `존재하지 않는 스레드에 메시지를 보내면 예외가 발생한다`() {
        val request = ChatMessageRequest(
            roomId = 1L,
            senderId = 2L,
            content = MessageContentRequest("hi", MessageType.TEXT),
            threadId = "5f9f1b9b9c9d1b9b9c9d1b9b"
        )

        `when`(loadMessagePort.findById(request.threadId!!.toObjectId())).thenReturn(null)

        assertThrows<ResourceNotFoundException> {
            sendThreadMessageService.sendThreadMessage(request)
        }

        verify(loadMessagePort).findById(request.threadId!!.toObjectId())
        verifyNoInteractions(sendMessageUseCase)
    }

    @Test
    @DisplayName("스레드 메시지를 전송할 수 있다")
    fun `스레드 메시지를 전송할 수 있다`() {
        val threadId = "5f9f1b9b9c9d1b9b9c9d1b9b"
        val request = ChatMessageRequest(
            roomId = 1L,
            senderId = 2L,
            content = MessageContentRequest("hi", MessageType.TEXT),
            threadId = threadId
        )

        val rootMessage = ChatMessage(
            id = MessageId.from(threadId),
            roomId = 1L,
            senderId = 3L,
            content = MessageContent("root", MessageType.TEXT),
            status = MessageStatus.SAVED,
            createdAt = Instant.now()
        )

        `when`(loadMessagePort.findById(threadId.toObjectId())).thenReturn(rootMessage)

        sendThreadMessageService.sendThreadMessage(request)

        verify(loadMessagePort).findById(threadId.toObjectId())
        verify(sendMessageUseCase).sendMessage(request)
    }
}
