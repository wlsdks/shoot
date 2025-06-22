package com.stark.shoot.application.service.message

import com.stark.shoot.application.filter.message.chain.DefaultMessageProcessingChain
import com.stark.shoot.domain.chat.message.ChatMessage
import com.stark.shoot.domain.chat.message.type.MessageStatus
import com.stark.shoot.domain.chat.message.type.MessageType
import com.stark.shoot.domain.chat.message.vo.MessageContent
import com.stark.shoot.infrastructure.config.redis.RedisLockManager
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.*
import java.time.Instant
import java.util.*

@DisplayName("메시지 처리 서비스 테스트")
class MessageProcessingServiceTest {

    // 테스트용 RedisLockManager 구현
    class TestRedisLockManager : RedisLockManager(mock(org.springframework.data.redis.core.StringRedisTemplate::class.java)) {
        var lockKeyUsed: String? = null
        var ownerIdUsed: String? = null

        override fun <T> withLock(lockKey: String, ownerId: String, action: () -> T): T {
            lockKeyUsed = lockKey
            ownerIdUsed = ownerId
            return action()
        }
    }

    @Test
    @DisplayName("메시지를 처리하고 결과를 반환할 수 있다")
    fun `메시지를 처리하고 결과를 반환할 수 있다`() {
        // given
        val messageProcessingChain = mock(DefaultMessageProcessingChain::class.java)
        val redisLockManager = TestRedisLockManager()

        val messageProcessingService = MessageProcessingService(
            messageProcessingChain,
            redisLockManager
        )

        val messageId = UUID.randomUUID().toString()
        val roomId = 1L
        val senderId = 2L
        val content = "테스트 메시지"
        val messageType = MessageType.TEXT

        val inputMessage = ChatMessage(
            id = messageId,
            roomId = roomId,
            senderId = senderId,
            content = MessageContent(content, messageType),
            status = MessageStatus.SENDING,
            createdAt = Instant.now()
        )

        val processedMessage = inputMessage.copy(
            status = MessageStatus.SAVED
        )

        // Mock the chain to return the processed message
        `when`(messageProcessingChain.reset()).thenReturn(messageProcessingChain)
        `when`(messageProcessingChain.proceed(inputMessage)).thenReturn(processedMessage)

        // when
        val result = messageProcessingService.processMessageCreate(inputMessage)

        // then
        assertThat(result).isEqualTo(processedMessage)
        assertThat(result.status).isEqualTo(MessageStatus.SAVED)

        // Verify lock key format
        assertThat(redisLockManager.lockKeyUsed).isEqualTo("chatroom:$roomId")
        assertThat(redisLockManager.ownerIdUsed).isNotNull()

        // Verify interactions
        verify(messageProcessingChain).reset()
        verify(messageProcessingChain).proceed(inputMessage)
    }

    @Test
    @DisplayName("처리 중 예외가 발생하면 예외를 전파한다")
    fun `처리 중 예외가 발생하면 예외를 전파한다`() {
        // given
        val messageProcessingChain = mock(DefaultMessageProcessingChain::class.java)
        val redisLockManager = TestRedisLockManager()

        val messageProcessingService = MessageProcessingService(
            messageProcessingChain,
            redisLockManager
        )

        val messageId = UUID.randomUUID().toString()
        val roomId = 1L
        val senderId = 2L
        val content = "테스트 메시지"
        val messageType = MessageType.TEXT

        val inputMessage = ChatMessage(
            id = messageId,
            roomId = roomId,
            senderId = senderId,
            content = MessageContent(content, messageType),
            status = MessageStatus.SENDING,
            createdAt = Instant.now()
        )

        // Mock the chain to throw an exception
        val expectedException = RuntimeException("메시지 처리 실패")
        `when`(messageProcessingChain.reset()).thenReturn(messageProcessingChain)
        `when`(messageProcessingChain.proceed(inputMessage)).thenThrow(expectedException)

        // when & then
        val exception = assertThrows<RuntimeException> {
            messageProcessingService.processMessageCreate(inputMessage)
        }

        assertThat(exception.message).isEqualTo("메시지 처리 실패")

        // Verify lock key format
        assertThat(redisLockManager.lockKeyUsed).isEqualTo("chatroom:$roomId")
        assertThat(redisLockManager.ownerIdUsed).isNotNull()

        // Verify interactions
        verify(messageProcessingChain).reset()
        verify(messageProcessingChain).proceed(inputMessage)
    }
}
