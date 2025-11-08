package com.stark.shoot.domain.chat.event

import com.stark.shoot.domain.chat.message.ChatMessage
import com.stark.shoot.domain.chat.message.vo.MessageId
import com.stark.shoot.domain.chat.vo.ChatRoomId
import com.stark.shoot.domain.shared.event.*
import com.stark.shoot.domain.shared.event.type.EventType
import com.stark.shoot.domain.shared.UserId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("채팅 이벤트 팩토리 메서드 테스트")
class EventFactoryTest {
    @Test
    @DisplayName("[happy] ChatEvent fromMessage 함수는 메시지를 기반으로 이벤트를 생성한다")
    fun `ChatEvent fromMessage 함수는 메시지를 기반으로 이벤트를 생성한다`() {
        val message = ChatMessage.create(roomId = ChatRoomId.from(1L), senderId = UserId.from(2L), text = "hi")
        val event = MessageEvent.fromMessage(message)

        assertThat(event.type).isEqualTo(EventType.MESSAGE_CREATED)
        assertThat(event.data).isEqualTo(message)
    }

    @Test
    @DisplayName("[happy] ChatBulkReadEvent create 함수는 주어진 값으로 이벤트를 생성한다")
    fun `ChatBulkReadEvent create 함수는 주어진 값으로 이벤트를 생성한다`() {
        val event = MessageBulkReadEvent.create(
            ChatRoomId.from(1L),
            listOf(MessageId.from("m1"), MessageId.from("m2")),
            UserId.from(3L)
        )
        assertThat(event).isEqualTo(
            MessageBulkReadEvent(
                ChatRoomId.from(1L),
                listOf(MessageId.from("m1"), MessageId.from("m2")),
                UserId.from(3L)
            )
        )
    }

    @Test
    @DisplayName("[happy] MessageSentEvent create 함수는 주어진 값으로 이벤트를 생성한다")
    fun `MessageSendedEvent create 함수는 주어진 값으로 이벤트를 생성한다`() {
        val message = ChatMessage.create(ChatRoomId.from(1L), UserId.from(2L), "hello")
        val event = MessageSentEvent.create(message)
        assertThat(event).isEqualTo(MessageSentEvent(message))
    }

    @Test
    @DisplayName("[happy] FriendAddedEvent create 함수는 주어진 값으로 이벤트를 생성한다")
    fun `FriendAddedEvent create 함수는 주어진 값으로 이벤트를 생성한다`() {
        val event = FriendAddedEvent.create(UserId.from(1L), UserId.from(2L))
        assertThat(event).isEqualTo(FriendAddedEvent(UserId.from(1L), UserId.from(2L)))
    }

    @Test
    @DisplayName("[happy] FriendRemovedEvent create 함수는 주어진 값으로 이벤트를 생성한다")
    fun `FriendRemovedEvent create 함수는 주어진 값으로 이벤트를 생성한다`() {
        val event = FriendRemovedEvent.create(UserId.from(1L), UserId.from(2L))
        assertThat(event).isEqualTo(FriendRemovedEvent(UserId.from(1L), UserId.from(2L)))
    }

    @Test
    @DisplayName("[happy] ChatRoomCreatedEvent create 함수는 주어진 값으로 이벤트를 생성한다")
    fun `ChatRoomCreatedEvent create 함수는 주어진 값으로 이벤트를 생성한다`() {
        val event = ChatRoomCreatedEvent.create(ChatRoomId.from(1L), UserId.from(2L))
        assertThat(event).isEqualTo(ChatRoomCreatedEvent(ChatRoomId.from(1L), UserId.from(2L)))
    }

    @Test
    @DisplayName("[happy] MessagePinEvent create 함수는 주어진 값으로 이벤트를 생성한다")
    fun `MessagePinEvent create 함수는 주어진 값으로 이벤트를 생성한다`() {
        val event = MessagePinEvent.create(
            MessageId.from("m1"),
            ChatRoomId.from(1L),
            true,
            UserId.from(2L)
        )
        assertThat(event).isEqualTo(
            MessagePinEvent(MessageId.from("m1"), ChatRoomId.from(1L), true, UserId.from(2L))
        )
    }

    @Test
    @DisplayName("[happy] MessageReactionEvent create 함수는 주어진 값으로 이벤트를 생성한다")
    fun `MessageReactionEvent create 함수는 주어진 값으로 이벤트를 생성한다`() {
        val event = MessageReactionEvent.create(MessageId.from("m1"), ChatRoomId.from(1L), UserId.from(2L), "like", true)
        assertThat(event).isEqualTo(
            MessageReactionEvent(MessageId.from("m1"), ChatRoomId.from(1L), UserId.from(2L), "like", true, false)
        )
    }
}
