package com.stark.shoot.adapter.`in`.web.chatroom

import com.stark.shoot.adapter.`in`.web.dto.chatroom.ChatRoomResponse
import com.stark.shoot.application.port.`in`.chatroom.ChatRoomSearchUseCase
import com.stark.shoot.application.port.`in`.chatroom.FindChatRoomUseCase
import com.stark.shoot.domain.user.vo.UserId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@DisplayName("ChatRoomSearchController 단위 테스트")
class ChatRoomSearchControllerTest {

    private val chatRoomSearchUseCase = mock(ChatRoomSearchUseCase::class.java)
    private val findChatRoomUseCase = mock(FindChatRoomUseCase::class.java)
    private val controller = ChatRoomSearchController(chatRoomSearchUseCase, findChatRoomUseCase)

    @Test
    @DisplayName("[happy] 채팅방을 검색한다")
    fun `채팅방을 검색한다`() {
        // given
        val userId = 1L
        val query = "테스트"
        val type = "GROUP"
        val unreadOnly = true

        val chatRooms = listOf(
            createChatRoomResponse(
                roomId = 1L,
                title = "테스트 채팅방 1",
                lastMessage = "안녕하세요",
                unreadMessages = 2,
                isPinned = true
            ),
            createChatRoomResponse(
                roomId = 2L,
                title = "테스트 채팅방 2",
                lastMessage = "반갑습니다",
                unreadMessages = 3,
                isPinned = false
            )
        )

        `when`(chatRoomSearchUseCase.searchChatRooms(
            UserId.from(userId),
            query,
            type,
            unreadOnly
        )).thenReturn(chatRooms)

        // when
        val response = controller.searchChatRooms(userId, query, type, unreadOnly)

        // then
        assertThat(response).isNotNull
        assertThat(response.success).isTrue()
        assertThat(response.data).hasSize(2)
        assertThat(response.data).isEqualTo(chatRooms)
        
        // 첫 번째 채팅방 검증
        assertThat(response.data?.get(0)?.roomId).isEqualTo(1L)
        assertThat(response.data?.get(0)?.title).isEqualTo("테스트 채팅방 1")
        
        // 두 번째 채팅방 검증
        assertThat(response.data?.get(1)?.roomId).isEqualTo(2L)
        assertThat(response.data?.get(1)?.title).isEqualTo("테스트 채팅방 2")

        verify(chatRoomSearchUseCase).searchChatRooms(
            UserId.from(userId),
            query,
            type,
            unreadOnly
        )
    }

    @Test
    @DisplayName("[happy] 검색 결과가 없는 경우 빈 목록을 반환한다")
    fun `검색 결과가 없는 경우 빈 목록을 반환한다`() {
        // given
        val userId = 1L
        val query = "존재하지 않는 채팅방"

        `when`(chatRoomSearchUseCase.searchChatRooms(
            UserId.from(userId),
            query,
            null,
            null
        )).thenReturn(emptyList())

        // when
        val response = controller.searchChatRooms(userId, query, null, null)

        // then
        assertThat(response).isNotNull
        assertThat(response.success).isTrue()
        assertThat(response.data).isEmpty()

        verify(chatRoomSearchUseCase).searchChatRooms(
            UserId.from(userId),
            query,
            null,
            null
        )
    }

    @Test
    @DisplayName("[happy] 두 사용자 간의 1:1 채팅방을 찾는다")
    fun `두 사용자 간의 1대1 채팅방을 찾는다`() {
        // given
        val myId = 1L
        val otherUserId = 2L

        val chatRoomResponse = createChatRoomResponse(
            roomId = 3L,
            title = "1:1 채팅방",
            lastMessage = "안녕하세요",
            unreadMessages = 0,
            isPinned = false
        )

        `when`(findChatRoomUseCase.findDirectChatBetweenUsers(
            UserId.from(myId),
            UserId.from(otherUserId)
        )).thenReturn(chatRoomResponse)

        // when
        val response = controller.findDirectChatRoom(myId, otherUserId)

        // then
        assertThat(response).isNotNull
        assertThat(response.success).isTrue()
        assertThat(response.data).isEqualTo(chatRoomResponse)
        assertThat(response.message).isEqualTo("채팅방을 찾았습니다.")

        verify(findChatRoomUseCase).findDirectChatBetweenUsers(
            UserId.from(myId),
            UserId.from(otherUserId)
        )
    }

    @Test
    @DisplayName("[fail] 두 사용자 간의 1:1 채팅방이 없는 경우 실패 응답을 반환한다")
    fun `두 사용자 간의 1대1 채팅방이 없는 경우 실패 응답을 반환한다`() {
        // given
        val myId = 1L
        val otherUserId = 3L

        `when`(findChatRoomUseCase.findDirectChatBetweenUsers(
            UserId.from(myId),
            UserId.from(otherUserId)
        )).thenReturn(null)

        // when
        val response = controller.findDirectChatRoom(myId, otherUserId)

        // then
        assertThat(response).isNotNull
        assertThat(response.success).isFalse()
        assertThat(response.data).isNull()
        assertThat(response.message).isEqualTo("채팅방을 찾을 수 없습니다.")
        assertThat(response.code).isEqualTo(404)

        verify(findChatRoomUseCase).findDirectChatBetweenUsers(
            UserId.from(myId),
            UserId.from(otherUserId)
        )
    }

    // 테스트용 ChatRoomResponse 객체 생성 헬퍼 메서드
    private fun createChatRoomResponse(
        roomId: Long,
        title: String,
        lastMessage: String?,
        unreadMessages: Int,
        isPinned: Boolean,
        timestamp: String = Instant.now().atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("a h:mm"))
    ): ChatRoomResponse {
        return ChatRoomResponse(
            roomId = roomId,
            title = title,
            lastMessage = lastMessage,
            unreadMessages = unreadMessages,
            isPinned = isPinned,
            timestamp = timestamp
        )
    }
}