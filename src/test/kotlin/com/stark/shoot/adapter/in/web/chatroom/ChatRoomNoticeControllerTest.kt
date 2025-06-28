package com.stark.shoot.adapter.`in`.web.chatroom

import com.stark.shoot.adapter.`in`.web.dto.chatroom.AnnouncementRequest
import com.stark.shoot.application.port.`in`.chatroom.ManageChatRoomUseCase
import com.stark.shoot.application.port.`in`.chatroom.command.UpdateAnnouncementCommand
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify

@DisplayName("ChatRoomNoticeController 단위 테스트")
class ChatRoomNoticeControllerTest {

    private val manageChatRoomUseCase = mock(ManageChatRoomUseCase::class.java)
    private val controller = ChatRoomNoticeController(manageChatRoomUseCase)

    @Test
    @DisplayName("[happy] 채팅방 공지사항을 설정한다")
    fun `채팅방 공지사항을 설정한다`() {
        // given
        val roomId = 1L
        val announcement = "중요한 공지사항입니다."
        val request = AnnouncementRequest(announcement)

        // when
        val response = controller.updateAnnouncement(roomId, request)

        // then
        assertThat(response).isNotNull
        assertThat(response.success).isTrue()
        assertThat(response.message).isEqualTo("공지사항이 업데이트되었습니다.")

        val command = UpdateAnnouncementCommand.of(roomId, announcement)
        verify(manageChatRoomUseCase).updateAnnouncement(command)
    }

    @Test
    @DisplayName("[happy] 채팅방 공지사항을 제거한다")
    fun `채팅방 공지사항을 제거한다`() {
        // given
        val roomId = 1L
        val request = AnnouncementRequest(null)

        // when
        val response = controller.updateAnnouncement(roomId, request)

        // then
        assertThat(response).isNotNull
        assertThat(response.success).isTrue()
        assertThat(response.message).isEqualTo("공지사항이 업데이트되었습니다.")

        val command = UpdateAnnouncementCommand.of(roomId, null)
        verify(manageChatRoomUseCase).updateAnnouncement(command)
    }
}
