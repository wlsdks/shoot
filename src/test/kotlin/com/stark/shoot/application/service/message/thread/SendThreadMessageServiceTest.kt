package com.stark.shoot.application.service.message.thread

import com.stark.shoot.adapter.`in`.web.dto.message.ChatMessageRequest
import com.stark.shoot.adapter.`in`.web.dto.message.MessageContentRequest
import com.stark.shoot.application.port.out.kafka.KafkaMessagePublishPort
import com.stark.shoot.application.port.out.message.MessageQueryPort
import com.stark.shoot.application.port.out.message.PublishMessagePort
import com.stark.shoot.application.port.out.message.preview.CacheUrlPreviewPort
import com.stark.shoot.application.port.out.message.preview.ExtractUrlPort
import com.stark.shoot.domain.chat.message.ChatMessage
import com.stark.shoot.domain.chat.message.service.MessageDomainService
import com.stark.shoot.domain.chat.message.type.MessageStatus
import com.stark.shoot.domain.chat.message.type.MessageType
import com.stark.shoot.domain.chat.message.vo.MessageContent
import com.stark.shoot.domain.chat.message.vo.MessageId
import com.stark.shoot.domain.chatroom.vo.ChatRoomId
import com.stark.shoot.domain.user.vo.UserId
import com.stark.shoot.infrastructure.config.async.ApplicationCoroutineScope
import com.stark.shoot.infrastructure.exception.web.ResourceNotFoundException
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.*
import java.time.Instant

@DisplayName("스레드 메시지 전송 서비스 테스트")
class SendThreadMessageServiceTest {

    private val messageQueryPort = mock(MessageQueryPort::class.java)
    private val extractUrlPort = mock(ExtractUrlPort::class.java)
    private val cacheUrlPreviewPort = mock(CacheUrlPreviewPort::class.java)
    private val kafkaMessagePublishPort = mock(KafkaMessagePublishPort::class.java)
    private val publishMessagePort = mock(PublishMessagePort::class.java)
    private val applicationCoroutineScope = mock(ApplicationCoroutineScope::class.java)
    private val messageDomainService = mock(MessageDomainService::class.java)

    private val sendThreadMessageService = SendThreadMessageService(
        messageQueryPort,
        extractUrlPort,
        cacheUrlPreviewPort,
        kafkaMessagePublishPort,
        publishMessagePort,
        applicationCoroutineScope,
        messageDomainService
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

        val threadId = MessageId.from(request.threadId!!)
        `when`(messageQueryPort.findById(threadId)).thenReturn(null)

        assertThrows<ResourceNotFoundException> {
            sendThreadMessageService.sendThreadMessage(request)
        }

        verify(messageQueryPort).findById(threadId)
        verifyNoInteractions(messageDomainService)
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
            roomId = ChatRoomId.from(1L),
            senderId = UserId.from(3L),
            content = MessageContent("root", MessageType.TEXT),
            status = MessageStatus.SAVED,
            createdAt = Instant.now()
        )

        val messageId = MessageId.from(threadId)
        `when`(messageQueryPort.findById(messageId)).thenReturn(rootMessage)

        // Mock the domain service to return a message
        val domainMessage = mock(ChatMessage::class.java)
        `when`(
            messageDomainService.createAndProcessMessage(
                eq(ChatRoomId.from(request.roomId)),
                eq(UserId.from(request.senderId)),
                eq(request.content.text),
                eq(request.content.type),
                any(),
                any(),
                any()
            )
        ).thenReturn(domainMessage)

        sendThreadMessageService.sendThreadMessage(request)

        verify(messageQueryPort).findById(messageId)
        verify(messageDomainService).createAndProcessMessage(
            eq(ChatRoomId.from(request.roomId)),
            eq(UserId.from(request.senderId)),
            eq(request.content.text),
            eq(request.content.type),
            any(),
            any(),
            any()
        )
    }
}
