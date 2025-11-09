package com.stark.shoot.domain.service.chatroom

import com.stark.shoot.domain.chatroom.ChatRoom
import com.stark.shoot.domain.chatroom.service.ChatRoomDomainService
import com.stark.shoot.domain.chatroom.type.ChatRoomType
import com.stark.shoot.domain.chatroom.vo.ChatRoomId
import com.stark.shoot.domain.chatroom.vo.MessageId
import com.stark.shoot.domain.chatroom.vo.ChatRoomTitle
import com.stark.shoot.domain.shared.UserId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.Instant

@DisplayName("채팅방 도메인 서비스 테스트")
class ChatRoomDomainServiceTest {
    private val service = ChatRoomDomainService()

    @Test
    @DisplayName("[happy] 채팅방 리스트를 제목으로 필터링할 수 있다")
    fun `채팅방 리스트를 제목으로 필터링할 수 있다`() {
        val rooms = listOf(
            ChatRoom(
                id = ChatRoomId.from(1L),
                title = ChatRoomTitle.from("hello"),
                type = ChatRoomType.GROUP,
                participants = mutableSetOf(UserId.from(1L))
            ),
            ChatRoom(
                id = ChatRoomId.from(2L),
                title = ChatRoomTitle.from("world"),
                type = ChatRoomType.GROUP,
                participants = mutableSetOf(UserId.from(1L))
            )
        )
        val result = service.filterChatRooms(rooms, "hello", null, null)
        assertThat(result.map { it.id }).containsExactly(ChatRoomId.from(1L))
    }

    @Test
    @DisplayName("[happy] 채팅방 제목 맵을 준비할 수 있다")
    fun `채팅방 제목 맵을 준비할 수 있다`() {
        val now = Instant.now()
        val rooms = listOf(
            ChatRoom(
                id = ChatRoomId.from(1L),
                title = ChatRoomTitle.from("room1"),
                type = ChatRoomType.GROUP,
                participants = mutableSetOf(UserId.from(1L)),
                lastActiveAt = now
            ),
            ChatRoom(
                id = ChatRoomId.from(2L),
                title = null,
                type = ChatRoomType.INDIVIDUAL,
                participants = mutableSetOf(UserId.from(1L), UserId.from(2L)),
                lastActiveAt = now
            )
        )
        val titles = service.prepareChatRoomTitles(rooms, UserId.from(1L))
        assertThat(titles[1L]).isEqualTo("room1")
        assertThat(titles[2L]).isEqualTo("1:1 채팅방")
    }

    @Test
    @DisplayName("[happy] 두 사용자 간 1대1 채팅방을 찾을 수 있다")
    fun `두 사용자 간 1대1 채팅방을 찾을 수 있다`() {
        val direct =
            ChatRoom(
                id = ChatRoomId.from(1L),
                title = ChatRoomTitle.from("dm"),
                type = ChatRoomType.INDIVIDUAL,
                participants = mutableSetOf(UserId.from(1L), UserId.from(2L))
            )
        val group =
            ChatRoom(
                id = ChatRoomId.from(2L),
                title = ChatRoomTitle.from("group"),
                type = ChatRoomType.GROUP,
                participants = mutableSetOf(UserId.from(1L), UserId.from(2L), UserId.from(3L))
            )

        val found = service.findDirectChatBetween(listOf(direct, group), UserId.from(1L), UserId.from(2L))

        assertThat(found).isEqualTo(direct)
    }

    @Test
    @DisplayName("[happy] 1대1 채팅방이 없으면 null을 반환한다")
    fun `1대1 채팅방이 없으면 null을 반환한다`() {
        val group =
            ChatRoom(
                id = ChatRoomId.from(1L),
                title = ChatRoomTitle.from("group"),
                type = ChatRoomType.GROUP,
                participants = mutableSetOf(UserId.from(1L), UserId.from(2L), UserId.from(3L))
            )

        val found = service.findDirectChatBetween(listOf(group), UserId.from(1L), UserId.from(2L))

        assertThat(found).isNull()
    }

    @Test
    @DisplayName("[happy] 마지막 메시지 맵을 준비할 수 있다")
    fun `마지막 메시지 맵을 준비할 수 있다`() {
        val rooms = listOf(
            ChatRoom(
                id = ChatRoomId.from(1L),
                title = ChatRoomTitle.from("room"),
                type = ChatRoomType.GROUP,
                participants = mutableSetOf(UserId.from(1L)),
                lastMessageId = MessageId.from("m1")
            ),
            ChatRoom(
                id = ChatRoomId.from(2L),
                title = ChatRoomTitle.from("none"),
                type = ChatRoomType.GROUP,
                participants = mutableSetOf(UserId.from(1L))
            )
        )

        val result = service.prepareLastMessages(rooms)

        assertThat(result[1L]).isEqualTo("최근 메시지")
        assertThat(result[2L]).isEqualTo("최근 메시지가 없습니다.")
    }

    @Test
    @DisplayName("[happy] 타임스탬프 맵을 준비할 수 있다")
    fun `타임스탬프 맵을 준비할 수 있다`() {
        val rooms = listOf(
            ChatRoom(
                id = ChatRoomId.from(1L),
                title = ChatRoomTitle.from("room"),
                type = ChatRoomType.GROUP,
                participants = mutableSetOf(UserId.from(1L)),
            )
        )

        val result = service.prepareTimestamps(rooms)

        assertThat(result[1L]).isNotBlank()
    }
}
