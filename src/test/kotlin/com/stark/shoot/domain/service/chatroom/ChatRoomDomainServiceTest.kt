package com.stark.shoot.domain.service.chatroom

import com.stark.shoot.domain.chatroom.ChatRoom
import com.stark.shoot.domain.chatroom.type.ChatRoomType
import com.stark.shoot.domain.chatroom.service.ChatRoomDomainService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.Instant

@DisplayName("채팅방 도메인 서비스 테스트")
class ChatRoomDomainServiceTest {
    private val service = ChatRoomDomainService()

    @Test
    fun `채팅방 리스트를 제목으로 필터링할 수 있다`() {
        val rooms = listOf(
            ChatRoom(id = 1L, title = "hello", type = ChatRoomType.GROUP, participants = mutableSetOf(1L)),
            ChatRoom(id = 2L, title = "world", type = ChatRoomType.GROUP, participants = mutableSetOf(1L))
        )
        val result = service.filterChatRooms(rooms, "hello", null, null, 1L)
        assertThat(result.map { it.id }).containsExactly(1L)
    }

    @Test
    fun `채팅방 제목 맵을 준비할 수 있다`() {
        val now = Instant.now()
        val rooms = listOf(
            ChatRoom(id = 1L, title = "room1", type = ChatRoomType.GROUP, participants = mutableSetOf(1L), lastActiveAt = now),
            ChatRoom(id = 2L, title = null, type = ChatRoomType.INDIVIDUAL, participants = mutableSetOf(1L,2L), lastActiveAt = now)
        )
        val titles = service.prepareChatRoomTitles(rooms, 1L)
        assertThat(titles[1L]).isEqualTo("room1")
        assertThat(titles[2L]).isEqualTo("1:1 채팅방")
    }
}
