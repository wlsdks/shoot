package com.stark.shoot.domain.chat.event

import com.stark.shoot.domain.chat.message.vo.MessageId
import com.stark.shoot.domain.chat.vo.ChatRoomId
import com.stark.shoot.domain.chatroom.vo.ChatRoomId as ChatRoomIdService
import com.stark.shoot.domain.shared.event.*
import com.stark.shoot.domain.shared.UserId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("채팅 이벤트 팩토리 메서드 테스트")
class EventFactoryTest {
    @Test
    @DisplayName("[happy] MessageBulkReadEvent create 함수는 주어진 값으로 이벤트를 생성한다")
    fun `MessageBulkReadEvent create 함수는 주어진 값으로 이벤트를 생성한다`() {
        val event = MessageBulkReadEvent.create(
            ChatRoomId.from(1L),
            listOf(MessageId.from("m1"), MessageId.from("m2")),
            UserId.from(3L)
        )
        assertThat(event).isEqualTo(
            MessageBulkReadEvent(
                roomId = ChatRoomId.from(1L),
                messageIds = listOf(MessageId.from("m1"), MessageId.from("m2")),
                userId = UserId.from(3L)
            )
        )
    }

    @Test
    @DisplayName("[happy] FriendAddedEvent create 함수는 주어진 값으로 이벤트를 생성한다")
    fun `FriendAddedEvent create 함수는 주어진 값으로 이벤트를 생성한다`() {
        val event = FriendAddedEvent.create(UserId.from(1L), UserId.from(2L))
        assertThat(event).isEqualTo(FriendAddedEvent(userId = UserId.from(1L), friendId = UserId.from(2L)))
    }

    @Test
    @DisplayName("[happy] FriendRemovedEvent create 함수는 주어진 값으로 이벤트를 생성한다")
    fun `FriendRemovedEvent create 함수는 주어진 값으로 이벤트를 생성한다`() {
        val event = FriendRemovedEvent.create(UserId.from(1L), UserId.from(2L))
        assertThat(event).isEqualTo(FriendRemovedEvent(userId = UserId.from(1L), friendId = UserId.from(2L)))
    }

    @Test
    @DisplayName("[happy] ChatRoomCreatedEvent create 함수는 주어진 값으로 이벤트를 생성한다")
    fun `ChatRoomCreatedEvent create 함수는 주어진 값으로 이벤트를 생성한다`() {
        val event = ChatRoomCreatedEvent.create(ChatRoomIdService.from(1L), UserId.from(2L))
        assertThat(event.roomId).isEqualTo(ChatRoomIdService.from(1L))
        assertThat(event.userId).isEqualTo(UserId.from(2L))
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
            MessagePinEvent(
                messageId = MessageId.from("m1"),
                roomId = ChatRoomId.from(1L),
                isPinned = true,
                userId = UserId.from(2L)
            )
        )
    }

    @Test
    @DisplayName("[happy] MessageReactionEvent create 함수는 주어진 값으로 이벤트를 생성한다")
    fun `MessageReactionEvent create 함수는 주어진 값으로 이벤트를 생성한다`() {
        val event = MessageReactionEvent.create(MessageId.from("m1"), ChatRoomId.from(1L), UserId.from(2L), "like", true)
        assertThat(event).isEqualTo(
            MessageReactionEvent(
                messageId = MessageId.from("m1"),
                roomId = ChatRoomId.from(1L),
                userId = UserId.from(2L),
                reactionType = "like",
                isAdded = true,
                isReplacement = false
            )
        )
    }
}
