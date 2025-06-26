package com.stark.shoot.adapter.`in`.web.chatroom

import com.stark.shoot.adapter.`in`.web.dto.chatroom.ChatRoomResponse
import com.stark.shoot.adapter.`in`.web.dto.chatroom.TitleRequest
import com.stark.shoot.application.port.`in`.chatroom.CreateChatRoomUseCase
import com.stark.shoot.application.port.`in`.chatroom.FindChatRoomUseCase
import com.stark.shoot.application.port.`in`.chatroom.ManageChatRoomUseCase
import com.stark.shoot.domain.chatroom.vo.ChatRoomId
import com.stark.shoot.domain.chatroom.vo.ChatRoomTitle
import com.stark.shoot.domain.user.vo.UserId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@DisplayName("ChatRoomController 단위 테스트")
class ChatRoomControllerTest {

    private val findChatRoomUseCase = mock(FindChatRoomUseCase::class.java)
    private val createChatRoomUseCase = mock(CreateChatRoomUseCase::class.java)
    private val manageChatRoomUseCase = mock(ManageChatRoomUseCase::class.java)
    private val controller = ChatRoomController(findChatRoomUseCase, createChatRoomUseCase, manageChatRoomUseCase)

    @Test
    @DisplayName("[happy] 1:1 채팅방 생성 요청을 처리하고 생성된 채팅방을 반환한다")
    fun `1대1 채팅방 생성 요청을 처리하고 생성된 채팅방을 반환한다`() {
        // given
        val userId = 1L
        val friendId = 2L
        val chatRoomResponse = createChatRoomResponse(1L, "채팅방", false)
        
        `when`(createChatRoomUseCase.createDirectChat(UserId.from(userId), UserId.from(friendId)))
            .thenReturn(chatRoomResponse)

        // when
        val response = controller.createDirectChat(userId, friendId)

        // then
        assertThat(response).isNotNull
        assertThat(response.success).isTrue()
        assertThat(response.data).isEqualTo(chatRoomResponse)
        assertThat(response.message).isEqualTo("채팅방이 생성되었습니다.")
        
        verify(createChatRoomUseCase).createDirectChat(UserId.from(userId), UserId.from(friendId))
    }

    @Test
    @DisplayName("[happy] 사용자의 채팅방 목록 조회 요청을 처리하고 채팅방 목록을 반환한다")
    fun `사용자의 채팅방 목록 조회 요청을 처리하고 채팅방 목록을 반환한다`() {
        // given
        val userId = 1L
        val chatRoomResponses = listOf(
            createChatRoomResponse(1L, "채팅방1", false),
            createChatRoomResponse(2L, "채팅방2", true)
        )
        
        `when`(findChatRoomUseCase.getChatRoomsForUser(UserId.from(userId)))
            .thenReturn(chatRoomResponses)

        // when
        val response = controller.getChatRooms(userId)

        // then
        assertThat(response).isNotNull
        assertThat(response.success).isTrue()
        assertThat(response.data).isEqualTo(chatRoomResponses)
        
        verify(findChatRoomUseCase).getChatRoomsForUser(UserId.from(userId))
    }

    @Test
    @DisplayName("[happy] 채팅방 퇴장 요청을 처리하고 성공 여부를 반환한다")
    fun `채팅방 퇴장 요청을 처리하고 성공 여부를 반환한다`() {
        // given
        val roomId = 1L
        val userId = 1L
        
        `when`(manageChatRoomUseCase.removeParticipant(ChatRoomId.from(roomId), UserId.from(userId)))
            .thenReturn(true)

        // when
        val response = controller.exitChatRoom(roomId, userId)

        // then
        assertThat(response).isNotNull
        assertThat(response.success).isTrue()
        assertThat(response.data).isTrue()
        assertThat(response.message).isEqualTo("채팅방에서 퇴장했습니다.")
        
        verify(manageChatRoomUseCase).removeParticipant(ChatRoomId.from(roomId), UserId.from(userId))
    }

    @Test
    @DisplayName("[happy] 채팅방 제목 변경 요청을 처리하고 성공 여부를 반환한다")
    fun `채팅방 제목 변경 요청을 처리하고 성공 여부를 반환한다`() {
        // given
        val roomId = 1L
        val newTitle = "새로운 채팅방 제목"
        val request = TitleRequest(newTitle)
        
        `when`(manageChatRoomUseCase.updateTitle(ChatRoomId.from(roomId), ChatRoomTitle.from(newTitle)))
            .thenReturn(true)

        // when
        val response = controller.updateTitle(roomId, request)

        // then
        assertThat(response).isNotNull
        assertThat(response.success).isTrue()
        assertThat(response.data).isTrue()
        assertThat(response.message).isEqualTo("채팅방 제목이 변경되었습니다.")
        
        verify(manageChatRoomUseCase).updateTitle(ChatRoomId.from(roomId), ChatRoomTitle.from(newTitle))
    }

    // 테스트용 ChatRoomResponse 객체 생성 헬퍼 메서드
    private fun createChatRoomResponse(
        roomId: Long,
        title: String,
        isPinned: Boolean
    ): ChatRoomResponse {
        val formatter = DateTimeFormatter.ofPattern("a h:mm")
        val timestamp = Instant.now().atZone(ZoneId.systemDefault()).let { formatter.format(it) }
        
        return ChatRoomResponse(
            roomId = roomId,
            title = title,
            lastMessage = "마지막 메시지",
            unreadMessages = 0,
            isPinned = isPinned,
            timestamp = timestamp
        )
    }
}