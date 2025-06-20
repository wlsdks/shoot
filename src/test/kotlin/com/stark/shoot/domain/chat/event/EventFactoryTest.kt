package com.stark.shoot.domain.chat.event

import com.stark.shoot.domain.chat.message.ChatMessage
import com.stark.shoot.domain.event.ChatRoomCreatedEvent
import com.stark.shoot.domain.event.FriendAddedEvent
import com.stark.shoot.domain.event.FriendRemovedEvent
import com.stark.shoot.domain.event.MessageBulkReadEvent
import com.stark.shoot.domain.event.MessageEvent
import com.stark.shoot.domain.event.MessagePinEvent
import com.stark.shoot.domain.event.MessageReactionEvent
import com.stark.shoot.domain.event.MessageUnreadCountUpdatedEvent
import com.stark.shoot.domain.chatroom.vo.ChatRoomId
import com.stark.shoot.domain.chat.message.vo.MessageId
import com.stark.shoot.domain.user.vo.UserId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("채팅 이벤트 팩토리 메서드 테스트")
class EventFactoryTest {
    @Test
    fun `ChatEvent fromMessage 함수는 메시지를 기반으로 이벤트를 생성한다`() {
        val message = ChatMessage.create(roomId = ChatRoomId.from(1L), senderId = UserId.from(2L), text = "hi")
        val event = MessageEvent.fromMessage(message)

        assertThat(event.type).isEqualTo(EventType.MESSAGE_CREATED)
        assertThat(event.data).isEqualTo(message)
    }

    @Test
    fun `ChatBulkReadEvent create 함수는 주어진 값으로 이벤트를 생성한다`() {
        val event = MessageBulkReadEvent.create(
            ChatRoomId.from(1L),
            listOf(MessageId.from("m1"), MessageId.from("m2")),
            UserId.from(3L)
        )
        assertThat(event).isEqualTo(
            MessageBulkReadEvent(ChatRoomId.from(1L), listOf(MessageId.from("m1"), MessageId.from("m2")), UserId.from(3L))
        )
    }

    @Test
    fun `ChatUnreadCountUpdatedEvent create 함수는 주어진 값으로 이벤트를 생성한다`() {
        val counts = mapOf(UserId.from(1L) to 2)
        val event = MessageUnreadCountUpdatedEvent.create(ChatRoomId.from(1L), counts)
        assertThat(event).isEqualTo(MessageUnreadCountUpdatedEvent(ChatRoomId.from(1L), counts, null))
    }

    @Test
    fun `FriendAddedEvent create 함수는 주어진 값으로 이벤트를 생성한다`() {
        val event = FriendAddedEvent.create(UserId.from(1L), UserId.from(2L))
        assertThat(event).isEqualTo(FriendAddedEvent(UserId.from(1L), UserId.from(2L)))
    }

    @Test
    fun `FriendRemovedEvent create 함수는 주어진 값으로 이벤트를 생성한다`() {
        val event = FriendRemovedEvent.create(UserId.from(1L), UserId.from(2L))
        assertThat(event).isEqualTo(FriendRemovedEvent(UserId.from(1L), UserId.from(2L)))
    }

    @Test
    fun `ChatRoomCreatedEvent create 함수는 주어진 값으로 이벤트를 생성한다`() {
        val event = ChatRoomCreatedEvent.create(ChatRoomId.from(1L), UserId.from(2L))
        assertThat(event).isEqualTo(ChatRoomCreatedEvent(ChatRoomId.from(1L), UserId.from(2L)))
    }

    @Test
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
    fun `MessageReactionEvent create 함수는 주어진 값으로 이벤트를 생성한다`() {
        val event = MessageReactionEvent.create("m1", "1", "2", "like", true)
        assertThat(event).isEqualTo(
            MessageReactionEvent("m1", "1", "2", "like", true, false)
        )
    }
}
