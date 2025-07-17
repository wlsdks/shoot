package com.stark.shoot.application.service.message

import com.stark.shoot.adapter.`in`.web.socket.WebSocketMessageBroker
import com.stark.shoot.application.port.`in`.message.command.ProcessMessageCommand
import com.stark.shoot.application.port.out.chatroom.ChatRoomCommandPort
import com.stark.shoot.application.port.out.chatroom.ChatRoomQueryPort
import com.stark.shoot.application.port.out.event.EventPublisher
import com.stark.shoot.application.port.out.message.SaveMessagePort
import com.stark.shoot.application.port.out.message.preview.CacheUrlPreviewPort
import com.stark.shoot.application.port.out.message.preview.ExtractUrlPort
import com.stark.shoot.application.port.out.message.preview.LoadUrlContentPort
import com.stark.shoot.domain.chat.message.ChatMessage
import com.stark.shoot.domain.chat.message.type.MessageStatus
import com.stark.shoot.domain.chat.message.type.MessageType
import com.stark.shoot.domain.chat.message.vo.ChatMessageMetadata
import com.stark.shoot.domain.chat.message.vo.MessageContent
import com.stark.shoot.domain.chat.message.vo.MessageId
import com.stark.shoot.domain.chatroom.ChatRoom
import com.stark.shoot.domain.chatroom.type.ChatRoomType
import com.stark.shoot.domain.chatroom.vo.ChatRoomId
import com.stark.shoot.domain.user.vo.UserId
import com.stark.shoot.infrastructure.config.redis.RedisLockManager
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.*
import java.time.Instant

@DisplayName("메시지 처리 서비스 테스트")
class MessageProcessingServiceTest {

    class TestRedisLockManager : RedisLockManager(mock()) {
        var lockKeyUsed: String? = null
        var ownerIdUsed: String? = null
        override fun <T> withLock(lockKey: String, ownerId: String, retryCount: Int, autoExtend: Boolean, action: () -> T): T {
            lockKeyUsed = lockKey
            ownerIdUsed = ownerId
            return action()
        }
    }

    @Test
    @DisplayName("[happy] 메시지를 처리하고 결과를 반환할 수 있다")
    fun `메시지를 처리하고 결과를 반환할 수 있다`() {
        val redisLockManager = TestRedisLockManager()
        val chatRoomQueryPort: ChatRoomQueryPort = mock()
        val chatRoomCommandPort: ChatRoomCommandPort = mock()
        val saveMessagePort: SaveMessagePort = mock()
        val extractUrlPort: ExtractUrlPort = mock()
        val loadUrlContentPort: LoadUrlContentPort = mock()
        val cacheUrlPreviewPort: CacheUrlPreviewPort = mock()
        val webSocketMessageBroker: WebSocketMessageBroker = mock()
        val eventPublisher: EventPublisher = mock()

        val service = MessageProcessingService(
            redisLockManager,
            chatRoomQueryPort,
            chatRoomCommandPort,
            saveMessagePort,
            extractUrlPort,
            loadUrlContentPort,
            cacheUrlPreviewPort,
            webSocketMessageBroker,
            eventPublisher,
        )

        val roomId = ChatRoomId.from(1L)
        val chatRoom = ChatRoom(id = roomId, type = ChatRoomType.GROUP, participants = mutableSetOf(UserId.from(2L)))
        whenever(chatRoomQueryPort.findById(roomId)).thenReturn(chatRoom)
        whenever(extractUrlPort.extractUrls(any())).thenReturn(emptyList())

        val inputMessage = ChatMessage(
            id = null,
            roomId = roomId,
            senderId = UserId.from(2L),
            content = MessageContent("hi", MessageType.TEXT),
            status = MessageStatus.SENDING,
            createdAt = Instant.now(),
            readBy = mutableMapOf(),
            metadata = ChatMessageMetadata()
        )

        val savedMessage = inputMessage.copy(id = MessageId.from("m1"), status = MessageStatus.SAVED)
        whenever(saveMessagePort.save(any())).thenReturn(savedMessage)

        val command = ProcessMessageCommand.of(inputMessage)
        val result = service.processMessageCreate(command)

        assertThat(result).isEqualTo(savedMessage)
        assertThat(redisLockManager.lockKeyUsed).isEqualTo("chatroom:${roomId}")
        verify(saveMessagePort).save(any())
        verify(webSocketMessageBroker).sendMessage("/topic/messages/${roomId.value}", savedMessage)
        verify(eventPublisher).publish(any())
    }

    @Test
    @DisplayName("[bad] 처리 중 예외가 발생하면 예외를 전파한다")
    fun `처리 중 예외가 발생하면 예외를 전파한다`() {
        val redisLockManager = TestRedisLockManager()
        val chatRoomQueryPort: ChatRoomQueryPort = mock()
        val chatRoomCommandPort: ChatRoomCommandPort = mock()
        val saveMessagePort: SaveMessagePort = mock()
        val extractUrlPort: ExtractUrlPort = mock()
        val loadUrlContentPort: LoadUrlContentPort = mock()
        val cacheUrlPreviewPort: CacheUrlPreviewPort = mock()
        val webSocketMessageBroker: WebSocketMessageBroker = mock()
        val eventPublisher: EventPublisher = mock()

        val service = MessageProcessingService(
            redisLockManager,
            chatRoomQueryPort,
            chatRoomCommandPort,
            saveMessagePort,
            extractUrlPort,
            loadUrlContentPort,
            cacheUrlPreviewPort,
            webSocketMessageBroker,
            eventPublisher,
        )

        val roomId = ChatRoomId.from(1L)
        whenever(chatRoomQueryPort.findById(roomId)).thenReturn(
            ChatRoom(id = roomId, type = ChatRoomType.GROUP, participants = mutableSetOf(UserId.from(2L)))
        )
        whenever(extractUrlPort.extractUrls(any())).thenReturn(emptyList())
        whenever(saveMessagePort.save(any())).thenThrow(RuntimeException("fail"))

        val message = ChatMessage.create(roomId, UserId.from(2L), "hi")
        val command = ProcessMessageCommand.of(message)

        val ex = assertThrows<RuntimeException> { service.processMessageCreate(command) }
        assertThat(ex.message).isEqualTo("fail")
        verify(saveMessagePort).save(any())
    }
}
