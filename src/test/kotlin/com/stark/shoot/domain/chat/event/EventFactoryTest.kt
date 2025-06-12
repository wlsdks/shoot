package com.stark.shoot.domain.chat.event

import com.stark.shoot.domain.chat.message.ChatMessage
import com.stark.shoot.domain.chat.message.type.MessageType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("채팅 이벤트 팩토리 메서드 테스트")
class EventFactoryTest {
    @Test
    fun `ChatEvent fromMessage 함수는 메시지를 기반으로 이벤트를 생성한다`() {
        val message = ChatMessage.create(roomId = 1L, senderId = 2L, text = "hi")
        val event = ChatEvent.fromMessage(message)

        assertThat(event.type).isEqualTo(EventType.MESSAGE_CREATED)
        assertThat(event.data).isEqualTo(message)
    }

    @Test
    fun `ChatBulkReadEvent create 함수는 주어진 값으로 이벤트를 생성한다`() {
        val event = ChatBulkReadEvent.create(1L, listOf("m1", "m2"), 3L)
        assertThat(event).isEqualTo(ChatBulkReadEvent(1L, listOf("m1", "m2"), 3L))
    }

    @Test
    fun `ChatUnreadCountUpdatedEvent create 함수는 주어진 값으로 이벤트를 생성한다`() {
        val counts = mapOf(1L to 2)
        val event = ChatUnreadCountUpdatedEvent.create(1L, counts)
        assertThat(event).isEqualTo(ChatUnreadCountUpdatedEvent(1L, counts, null))
    }

    @Test
    fun `FriendAddedEvent create 함수는 주어진 값으로 이벤트를 생성한다`() {
        val event = FriendAddedEvent.create(1L, 2L)
        assertThat(event).isEqualTo(FriendAddedEvent(1L, 2L))
    }

    @Test
    fun `ChatRoomCreatedEvent create 함수는 주어진 값으로 이벤트를 생성한다`() {
        val event = ChatRoomCreatedEvent.create(1L, 2L)
        assertThat(event).isEqualTo(ChatRoomCreatedEvent(1L, 2L))
    }

    @Test
    fun `MessagePinEvent create 함수는 주어진 값으로 이벤트를 생성한다`() {
        val event = MessagePinEvent.create("m1", 1L, true, 2L)
        assertThat(event).isEqualTo(MessagePinEvent("m1", 1L, true, 2L))
    }

    @Test
    fun `MessageReactionEvent create 함수는 주어진 값으로 이벤트를 생성한다`() {
        val event = MessageReactionEvent.create("m1", "1", "2", "like", true)
        assertThat(event).isEqualTo(
            MessageReactionEvent("m1", "1", "2", "like", true, false)
        )
    }
}
