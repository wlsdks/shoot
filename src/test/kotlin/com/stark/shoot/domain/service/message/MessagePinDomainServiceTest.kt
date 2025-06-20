package com.stark.shoot.domain.service.message

import com.stark.shoot.domain.event.MessagePinEvent
import com.stark.shoot.domain.chat.message.ChatMessage
import com.stark.shoot.domain.chat.message.service.MessagePinDomainService
import com.stark.shoot.domain.chat.message.vo.MessageContent
import com.stark.shoot.domain.chat.message.vo.MessageId
import com.stark.shoot.domain.chat.message.type.MessageStatus
import com.stark.shoot.domain.chat.message.type.MessageType
import com.stark.shoot.domain.chatroom.vo.ChatRoomId
import com.stark.shoot.domain.user.vo.UserId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.Instant

@DisplayName("메시지 핀 도메인 서비스 테스트")
class MessagePinDomainServiceTest {
    private val service = MessagePinDomainService()

    @Nested
    inner class CreatePinEvent {
        @Test
        fun `메시지 ID가 없으면 null을 반환한다`() {
            val message = ChatMessage(
                roomId = 1L,
                senderId = UserId.from(2L),
                content = MessageContent("hi", MessageType.TEXT),
                status = MessageStatus.SAVED,
                createdAt = Instant.now()
            )

            val event = service.createPinEvent(message, 1L, true)
            assertThat(event).isNull()
        }

        @Test
        fun `메시지 핀 이벤트를 생성할 수 있다`() {
            val message = ChatMessage(
                id = "m1",
                roomId = 1L,
                senderId = 2L,
                content = MessageContent("hi", MessageType.TEXT),
                status = MessageStatus.SAVED,
                createdAt = Instant.now()
            )

            val event = service.createPinEvent(message, UserId.from(1L), true)
            assertThat(event).isEqualTo(
                MessagePinEvent.create(
                    messageId = MessageId.from("m1"),
                    roomId = ChatRoomId.from(1L),
                    isPinned = true,
                    userId = UserId.from(1L)
                )
            )
        }
    }
}
