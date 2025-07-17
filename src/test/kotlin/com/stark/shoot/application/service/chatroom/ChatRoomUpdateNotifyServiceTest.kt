package com.stark.shoot.application.service.chatroom

import com.stark.shoot.application.port.out.chatroom.SendChatRoomUpdatePort
import com.stark.shoot.domain.chatroom.vo.ChatRoomId
import com.stark.shoot.domain.event.ChatRoomUpdateEvent
import com.stark.shoot.domain.user.vo.UserId
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.springframework.data.redis.core.ValueOperations
import org.springframework.data.redis.core.StringRedisTemplate

@DisplayName("ChatRoomUpdateNotifyService 테스트")
class ChatRoomUpdateNotifyServiceTest {

    private val redisTemplate: StringRedisTemplate = mock()
    private val valueOps: ValueOperations<String, String> = mock()
    private val sendPort: SendChatRoomUpdatePort = mock()
    private val service = ChatRoomUpdateNotifyService(redisTemplate, sendPort)

    @Test
    @DisplayName("활성 사용자가 아니면 업데이트를 전송한다")
    fun `inactive user receives update`() {
        val roomId = ChatRoomId.from(1L)
        val userId = UserId.from(2L)
        val update = ChatRoomUpdateEvent.Update(unreadCount = 3, lastMessage = "hi")
        val event = ChatRoomUpdateEvent.create(roomId, mapOf(userId to update))

        whenever(redisTemplate.opsForValue()).thenReturn(valueOps)
        whenever(valueOps.get("active:${userId.value}:${roomId.value}"))
            .thenReturn("false")

        service.notify(event)

        verify(sendPort).sendUpdate(userId, roomId, update)
    }

    @Test
    @DisplayName("활성 사용자이면 업데이트를 전송하지 않는다")
    fun `active user does not receive update`() {
        val roomId = ChatRoomId.from(1L)
        val userId = UserId.from(2L)
        val update = ChatRoomUpdateEvent.Update(unreadCount = 3, lastMessage = "hi")
        val event = ChatRoomUpdateEvent.create(roomId, mapOf(userId to update))

        whenever(redisTemplate.opsForValue()).thenReturn(valueOps)
        whenever(valueOps.get("active:${userId.value}:${roomId.value}"))
            .thenReturn("true")

        service.notify(event)

        verifyNoInteractions(sendPort)
    }
}
