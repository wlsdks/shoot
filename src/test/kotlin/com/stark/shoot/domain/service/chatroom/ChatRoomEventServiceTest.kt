package com.stark.shoot.domain.service.chatroom

import com.stark.shoot.domain.chat.event.ChatRoomCreatedEvent
import com.stark.shoot.domain.chat.room.ChatRoom
import com.stark.shoot.domain.chat.room.type.ChatRoomType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("채팅방 이벤트 도메인 서비스 테스트")
class ChatRoomEventServiceTest {
    private val service = ChatRoomEventService()

    @Test
    fun `채팅방 생성 이벤트를 생성할 수 있다`() {
        val room = ChatRoom(id = 1L, title = "room", type = ChatRoomType.GROUP, participants = mutableSetOf(1L,2L))
        val events = service.createChatRoomCreatedEvents(room)
        assertThat(events).hasSize(2)
        assertThat(events[0]).isEqualTo(ChatRoomCreatedEvent.create(1L, 1L))
        assertThat(events[1]).isEqualTo(ChatRoomCreatedEvent.create(1L, 2L))
    }

    @Test
    fun `ID가 없는 채팅방 이벤트 생성 시 예외가 발생한다`() {
        val room = ChatRoom(title = "room", type = ChatRoomType.GROUP, participants = mutableSetOf(1L))
        org.junit.jupiter.api.assertThrows<IllegalArgumentException> {
            service.createChatRoomCreatedEvents(room)
        }
    }
}
