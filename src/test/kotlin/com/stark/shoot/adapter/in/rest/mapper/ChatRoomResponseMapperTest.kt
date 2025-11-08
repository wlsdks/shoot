package com.stark.shoot.adapter.`in`.rest.mapper

import com.stark.shoot.domain.chatroom.ChatRoom
import com.stark.shoot.domain.chatroom.type.ChatRoomType
import com.stark.shoot.domain.chat.vo.ChatRoomId
import com.stark.shoot.domain.chatroom.vo.ChatRoomTitle
import com.stark.shoot.domain.shared.UserId
import com.stark.shoot.infrastructure.mapper.ChatRoomResponseMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.Instant
import org.hamcrest.Matchers.hasSize

@DisplayName("ChatRoomResponseMapper 테스트")
class ChatRoomResponseMapperTest {

    private val mapper = ChatRoomResponseMapper()

    @Test
    @DisplayName("[happy] 채팅방을 응답 DTO로 변환한다")
    fun `채팅방을 응답 DTO로 변환한다`() {
        val room = ChatRoom(
            id = ChatRoomId.from(1L),
            title = ChatRoomTitle.from("title"),
            type = ChatRoomType.GROUP,
            participants = mutableSetOf(UserId.from(1L), UserId.from(2L)),
            pinnedParticipants = mutableSetOf(UserId.from(1L)),
            lastActiveAt = Instant.parse("2024-01-01T00:00:00Z")
        )

        val response = mapper.toResponse(
            room,
            UserId.from(1L),
            "Room",
            "Hello",
            "time"
        )

        assertThat(response.roomId).isEqualTo(1L)
        assertThat(response.title).isEqualTo("Room")
        assertThat(response.lastMessage).isEqualTo("Hello")
        assertThat(response.isPinned).isTrue()
        assertThat(response.timestamp).isEqualTo("time")
    }

    @Test
    @DisplayName("[happy] 채팅방 목록을 응답 DTO 목록으로 변환한다")
    fun `채팅방 목록을 응답 DTO 목록으로 변환한다`() {
        val room = ChatRoom(
            id = ChatRoomId.from(2L),
            title = ChatRoomTitle.from("room"),
            type = ChatRoomType.GROUP,
            participants = mutableSetOf(UserId.from(1L)),
            lastActiveAt = Instant.parse("2024-01-01T00:00:00Z")
        )

        val responses = mapper.toResponseList(
            rooms = listOf(room),
            userId = UserId.from(1L),
            titles = mapOf(2L to "room"),
            lastMessages = mapOf(2L to "msg"),
            timestamps = mapOf(2L to "time")
        )

        assertThat(responses).hasSize(1)
        val response = responses.first()
        assertThat(response.roomId).isEqualTo(2L)
        assertThat(response.title).isEqualTo("room")
        assertThat(response.lastMessage).isEqualTo("msg")
        assertThat(response.timestamp).isEqualTo("time")
    }
}
