package com.stark.shoot.domain.service.chatroom

import com.stark.shoot.domain.chatroom.ChatRoom
import com.stark.shoot.domain.chatroom.service.ChatRoomEventService
import com.stark.shoot.domain.chatroom.type.ChatRoomType
import com.stark.shoot.domain.chat.vo.ChatRoomId
import com.stark.shoot.domain.chatroom.vo.ChatRoomTitle
import com.stark.shoot.domain.event.ChatRoomCreatedEvent
import com.stark.shoot.domain.shared.UserId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.hamcrest.Matchers.hasSize
import com.stark.shoot.infrastructure.exception.ChatRoomException

@DisplayName("채팅방 이벤트 도메인 서비스 테스트")
class ChatRoomEventServiceTest {
    private val service = ChatRoomEventService()

    @Test
    @DisplayName("[happy] 채팅방 생성 이벤트를 생성할 수 있다")
    fun `채팅방 생성 이벤트를 생성할 수 있다`() {
        val room = ChatRoom(
            id = ChatRoomId.from(1L),
            title = ChatRoomTitle.from("room"),
            type = ChatRoomType.GROUP,
            participants = mutableSetOf(UserId.from(1L), UserId.from(2L))
        )
        val events = service.createChatRoomCreatedEvents(room)
        assertThat(events).hasSize(2)
        assertThat(events[0]).isEqualTo(ChatRoomCreatedEvent.create(ChatRoomId.from(1L), UserId.from(1L)))
        assertThat(events[1]).isEqualTo(ChatRoomCreatedEvent.create(ChatRoomId.from(1L), UserId.from(2L)))
    }

    @Test
    @DisplayName("[bad] ID가 없는 채팅방 이벤트 생성 시 예외가 발생한다")
    fun `ID가 없는 채팅방 이벤트 생성 시 예외가 발생한다`() {
        val room = ChatRoom(
            title = ChatRoomTitle.from("room"),
            type = ChatRoomType.GROUP,
            participants = mutableSetOf(UserId.from(1L))
        )
        org.junit.jupiter.api.assertThrows<ChatRoomException.MissingId> {
            service.createChatRoomCreatedEvents(room)
        }
    }
}
