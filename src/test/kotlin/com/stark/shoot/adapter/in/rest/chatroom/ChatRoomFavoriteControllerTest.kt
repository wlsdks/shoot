package com.stark.shoot.adapter.`in`.rest.chatroom

import com.stark.shoot.adapter.`in`.rest.dto.chatroom.ChatRoomResponse
import com.stark.shoot.application.port.`in`.chatroom.UpdateChatRoomFavoriteUseCase
import com.stark.shoot.application.port.`in`.chatroom.command.UpdateFavoriteStatusCommand
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@DisplayName("ChatRoomFavoriteController 단위 테스트")
class ChatRoomFavoriteControllerTest {

    private val updateFavoriteUseCase = mock(UpdateChatRoomFavoriteUseCase::class.java)
    private val controller = ChatRoomFavoriteController(updateFavoriteUseCase)

    @Test
    @DisplayName("[happy] 채팅방을 즐겨찾기에 추가한다")
    fun `채팅방을 즐겨찾기에 추가한다`() {
        // given
        val roomId = 1L
        val userId = 2L
        val isFavorite = true

        val chatRoomResponse = createChatRoomResponse(
            roomId = roomId,
            title = "테스트 채팅방",
            isPinned = true
        )

        val command = UpdateFavoriteStatusCommand.of(roomId, userId, isFavorite)
        `when`(updateFavoriteUseCase.updateFavoriteStatus(command)).thenReturn(chatRoomResponse)

        // when
        val response = controller.updateFavorite(roomId, userId, isFavorite)

        // then
        assertThat(response).isNotNull
        assertThat(response.success).isTrue()
        assertThat(response.data).isEqualTo(chatRoomResponse)
        assertThat(response.message).isEqualTo("채팅방이 즐겨찾기에 추가되었습니다.")

        verify(updateFavoriteUseCase).updateFavoriteStatus(command)
    }

    @Test
    @DisplayName("[happy] 채팅방을 즐겨찾기에서 제거한다")
    fun `채팅방을 즐겨찾기에서 제거한다`() {
        // given
        val roomId = 1L
        val userId = 2L
        val isFavorite = false

        val chatRoomResponse = createChatRoomResponse(
            roomId = roomId,
            title = "테스트 채팅방",
            isPinned = false
        )

        val command = UpdateFavoriteStatusCommand.of(roomId, userId, isFavorite)
        `when`(updateFavoriteUseCase.updateFavoriteStatus(command)).thenReturn(chatRoomResponse)

        // when
        val response = controller.updateFavorite(roomId, userId, isFavorite)

        // then
        assertThat(response).isNotNull
        assertThat(response.success).isTrue()
        assertThat(response.data).isEqualTo(chatRoomResponse)
        assertThat(response.message).isEqualTo("채팅방이 즐겨찾기에서 제거되었습니다.")

        verify(updateFavoriteUseCase).updateFavoriteStatus(command)
    }

    // 테스트용 ChatRoomResponse 객체 생성 헬퍼 메서드
    private fun createChatRoomResponse(
        roomId: Long,
        title: String,
        lastMessage: String? = "마지막 메시지",
        unreadMessages: Int = 0,
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
