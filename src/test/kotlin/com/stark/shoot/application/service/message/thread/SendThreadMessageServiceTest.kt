package com.stark.shoot.application.service.message.thread

import com.stark.shoot.adapter.`in`.web.dto.message.ChatMessageRequest
import com.stark.shoot.adapter.`in`.web.dto.message.MessageContentRequest
import com.stark.shoot.application.port.`in`.message.thread.command.SendThreadMessageCommand
import com.stark.shoot.application.port.out.message.MessagePublisherPort
import com.stark.shoot.application.port.out.message.MessageQueryPort
import com.stark.shoot.application.port.out.message.preview.CacheUrlPreviewPort
import com.stark.shoot.application.port.out.message.preview.ExtractUrlPort
import com.stark.shoot.domain.chat.message.ChatMessage
import com.stark.shoot.domain.chat.message.service.MessageDomainService
import com.stark.shoot.domain.chat.message.type.MessageStatus
import com.stark.shoot.domain.chat.message.type.MessageType
import com.stark.shoot.domain.chat.message.vo.ChatMessageMetadata
import com.stark.shoot.domain.chat.message.vo.MessageContent
import com.stark.shoot.domain.chat.message.vo.MessageId
import com.stark.shoot.domain.chatroom.vo.ChatRoomId
import com.stark.shoot.domain.user.vo.UserId
import com.stark.shoot.infrastructure.config.async.ApplicationCoroutineScope
import com.stark.shoot.infrastructure.exception.web.ResourceNotFoundException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.*
import java.time.Instant

@DisplayName("스레드 메시지 전송 서비스 테스트")
class SendThreadMessageServiceTest {

    // Test implementation of ApplicationCoroutineScope
    class TestApplicationCoroutineScope : ApplicationCoroutineScope() {
        // Override launch to do nothing and return a dummy Job
        override fun launch(block: suspend CoroutineScope.() -> Unit): Job {
            return Job()
        }
    }

    private lateinit var messageQueryPort: MessageQueryPort
    private lateinit var extractUrlPort: ExtractUrlPort
    private lateinit var cacheUrlPreviewPort: CacheUrlPreviewPort
    private lateinit var messagePublisherPort: MessagePublisherPort
    private lateinit var messageDomainService: MessageDomainService
    private lateinit var sendThreadMessageService: SendThreadMessageService

    @BeforeEach
    fun setUp() {
        messageQueryPort = mock(MessageQueryPort::class.java)
        extractUrlPort = mock(ExtractUrlPort::class.java)
        cacheUrlPreviewPort = mock(CacheUrlPreviewPort::class.java)
        messagePublisherPort = mock(MessagePublisherPort::class.java)
        messageDomainService = mock(MessageDomainService::class.java)

        sendThreadMessageService = SendThreadMessageService(
            messageQueryPort,
            extractUrlPort,
            cacheUrlPreviewPort,
            messagePublisherPort,
            messageDomainService
        )
    }

    @Test
    @DisplayName("[bad] 존재하지 않는 스레드에 메시지를 보내면 예외가 발생한다")
    fun `존재하지 않는 스레드에 메시지를 보내면 예외가 발생한다`() {
        // given
        val request = ChatMessageRequest(
            roomId = 1L,
            senderId = 2L,
            content = MessageContentRequest("hi", MessageType.TEXT),
            threadId = "5f9f1b9b9c9d1b9b9c9d1b9b"
        )

        val threadId = MessageId.from(request.threadId!!)
        doReturn(null).`when`(messageQueryPort).findById(threadId)

        // when & then
        assertThrows<ResourceNotFoundException> {
            sendThreadMessageService.sendThreadMessage(SendThreadMessageCommand(request))
        }

        verify(messageQueryPort).findById(threadId)
        verifyNoInteractions(messageDomainService)
    }

    @Test
    @DisplayName("[happy] 스레드 메시지를 전송할 수 있다")
    fun `스레드 메시지를 전송할 수 있다`() {
        // given
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
        doReturn(rootMessage).`when`(messageQueryPort).findById(messageId)

        // Create a mock ChatMessage with necessary metadata
        val domainMessage = mock(ChatMessage::class.java)
        val metadata = ChatMessageMetadata()
        doReturn(metadata).`when`(domainMessage).metadata

        // Define function implementations for the function parameters
        val extractUrls: (String) -> List<String> = { _ -> emptyList() }
        val getCachedPreview: (String) -> ChatMessageMetadata.UrlPreview? = { _ -> null }

        // Setup the messageDomainService to return our mock message with actual parameters
        doReturn(domainMessage).`when`(messageDomainService).createAndProcessMessage(
            ChatRoomId.from(request.roomId),
            UserId.from(request.senderId),
            request.content.text,
            request.content.type,
            MessageId.from(threadId),
            extractUrls,
            getCachedPreview
        )

        // when
        sendThreadMessageService.sendThreadMessage(SendThreadMessageCommand(request))

        // then
        verify(messageQueryPort).findById(messageId)
    }
}
