package com.stark.shoot.domain.service.chatroom

import com.stark.shoot.domain.chatroom.ChatRoom
import com.stark.shoot.domain.chatroom.service.ChatRoomDomainService
import com.stark.shoot.domain.chatroom.type.ChatRoomType
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

    @Test
    fun `두 사용자 간 1대1 채팅방을 찾을 수 있다`() {
        val direct = ChatRoom(id = 1L, title = "dm", type = ChatRoomType.INDIVIDUAL, participants = mutableSetOf(1L,2L))
        val group = ChatRoom(id = 2L, title = "group", type = ChatRoomType.GROUP, participants = mutableSetOf(1L,2L,3L))

        val found = service.findDirectChatBetween(listOf(direct, group), 1L, 2L)

        assertThat(found).isEqualTo(direct)
    }

    @Test
    fun `1대1 채팅방이 없으면 null을 반환한다`() {
        val group = ChatRoom(id = 1L, title = "group", type = ChatRoomType.GROUP, participants = mutableSetOf(1L,2L,3L))

        val found = service.findDirectChatBetween(listOf(group), 1L, 2L)

        assertThat(found).isNull()
    }

    @Test
    fun `마지막 메시지 맵을 준비할 수 있다`() {
        val rooms = listOf(
            ChatRoom(id = 1L, title = "room", type = ChatRoomType.GROUP, participants = mutableSetOf(1L), lastMessageId = "m1"),
            ChatRoom(id = 2L, title = "none", type = ChatRoomType.GROUP, participants = mutableSetOf(1L))
        )

        val result = service.prepareLastMessages(rooms)

        assertThat(result[1L]).isEqualTo("최근 메시지")
        assertThat(result[2L]).isEqualTo("최근 메시지가 없습니다.")
    }

    @Test
    fun `타임스탬프 맵을 준비할 수 있다`() {
        val rooms = listOf(
            ChatRoom(id = 1L, title = "room", type = ChatRoomType.GROUP, participants = mutableSetOf(1L))
        )

        val result = service.prepareTimestamps(rooms)

        assertThat(result[1L]).isNotBlank()
    }
}
