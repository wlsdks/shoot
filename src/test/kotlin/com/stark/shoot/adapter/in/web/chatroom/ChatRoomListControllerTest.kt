package com.stark.shoot.adapter.`in`.web.chatroom

import com.stark.shoot.adapter.`in`.web.dto.chatroom.ChatRoomResponse
import com.stark.shoot.application.port.`in`.chatroom.FindChatRoomUseCase
import com.stark.shoot.domain.user.vo.UserId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@DisplayName("ChatRoomListController 단위 테스트")
class ChatRoomListControllerTest {

    private val findChatRoomUseCase = mock(FindChatRoomUseCase::class.java)
    private val controller = ChatRoomListController(findChatRoomUseCase)

    @Test
    @DisplayName("[happy] 사용자의 채팅방 목록을 조회한다")
    fun `사용자의 채팅방 목록을 조회한다`() {
        // given
        val userId = 1L

        val chatRooms = listOf(
            createChatRoomResponse(
                roomId = 1L,
                title = "첫 번째 채팅방",
                lastMessage = "안녕하세요",
                unreadMessages = 2,
                isPinned = true
            ),
            createChatRoomResponse(
                roomId = 2L,
                title = "두 번째 채팅방",
                lastMessage = "반갑습니다",
                unreadMessages = 0,
                isPinned = false
            ),
            createChatRoomResponse(
                roomId = 3L,
                title = "세 번째 채팅방",
                lastMessage = null,
                unreadMessages = 5,
                isPinned = true
            )
        )

        `when`(findChatRoomUseCase.getChatRoomsForUser(UserId.from(userId)))
            .thenReturn(chatRooms)

        // when
        val response = controller.getChatRooms(userId)

        // then
        assertThat(response).isNotNull
        assertThat(response.success).isTrue()
        assertThat(response.data).hasSize(3)
        assertThat(response.data).isEqualTo(chatRooms)
        
        // 첫 번째 채팅방 검증
        assertThat(response.data?.get(0)?.roomId).isEqualTo(1L)
        assertThat(response.data?.get(0)?.title).isEqualTo("첫 번째 채팅방")
        assertThat(response.data?.get(0)?.lastMessage).isEqualTo("안녕하세요")
        assertThat(response.data?.get(0)?.unreadMessages).isEqualTo(2)
        assertThat(response.data?.get(0)?.isPinned).isTrue()
        
        // 두 번째 채팅방 검증
        assertThat(response.data?.get(1)?.roomId).isEqualTo(2L)
        assertThat(response.data?.get(1)?.title).isEqualTo("두 번째 채팅방")
        assertThat(response.data?.get(1)?.lastMessage).isEqualTo("반갑습니다")
        assertThat(response.data?.get(1)?.unreadMessages).isEqualTo(0)
        assertThat(response.data?.get(1)?.isPinned).isFalse()

        verify(findChatRoomUseCase).getChatRoomsForUser(UserId.from(userId))
    }

    @Test
    @DisplayName("[happy] 사용자의 채팅방이 없는 경우 빈 목록을 반환한다")
    fun `사용자의 채팅방이 없는 경우 빈 목록을 반환한다`() {
        // given
        val userId = 1L

        `when`(findChatRoomUseCase.getChatRoomsForUser(UserId.from(userId)))
            .thenReturn(emptyList())

        // when
        val response = controller.getChatRooms(userId)

        // then
        assertThat(response).isNotNull
        assertThat(response.success).isTrue()
        assertThat(response.data).isEmpty()

        verify(findChatRoomUseCase).getChatRoomsForUser(UserId.from(userId))
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