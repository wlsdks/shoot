package com.stark.shoot.application.service.message.schedule

import com.stark.shoot.adapter.`in`.web.dto.ApiException
import com.stark.shoot.adapter.`in`.web.dto.ErrorCode
import com.stark.shoot.adapter.`in`.web.dto.message.schedule.ScheduledMessageResponseDto
import com.stark.shoot.adapter.out.persistence.mongodb.document.message.embedded.type.MessageType
import com.stark.shoot.adapter.out.persistence.mongodb.mapper.ScheduledMessageMapper
import com.stark.shoot.application.port.out.chatroom.LoadChatRoomPort
import com.stark.shoot.application.port.out.message.ScheduledMessagePort
import com.stark.shoot.domain.chat.message.MessageContent
import com.stark.shoot.domain.chat.message.ScheduledMessage
import com.stark.shoot.domain.chat.room.ChatRoom
import com.stark.shoot.domain.chat.room.ChatRoomType
import com.stark.shoot.infrastructure.enumerate.ScheduledMessageStatus
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.*
import org.mockito.ArgumentMatchers.any
import java.time.Instant
import java.time.temporal.ChronoUnit

@DisplayName("예약 메시지 서비스 테스트")
class ScheduledMessageServiceTest {

    private val scheduledMessagePort = mock(ScheduledMessagePort::class.java)
    private val loadChatRoomPort = mock(LoadChatRoomPort::class.java)
    private val scheduledMessageMapper = mock(ScheduledMessageMapper::class.java)

    private val sut = ScheduledMessageService(
        scheduledMessagePort,
        loadChatRoomPort,
        scheduledMessageMapper
    )

    @Test
    @DisplayName("존재하지 않는 채팅방에 메시지를 예약하면 예외가 발생한다")
    fun `존재하지 않는 채팅방에 메시지를 예약하면 예외가 발생한다`() {
        // given
        val roomId = 1L
        val senderId = 2L
        val content = "테스트 예약 메시지"
        val scheduledAt = Instant.now().plus(1, ChronoUnit.HOURS)

        doReturn(null).`when`(loadChatRoomPort).findById(roomId)

        // when & then
        val exception = assertThrows<ApiException> {
            sut.scheduleMessage(roomId, senderId, content, scheduledAt)
        }

        assertThat(exception.errorCode).isEqualTo(ErrorCode.ROOM_NOT_FOUND)
        verify(loadChatRoomPort).findById(roomId)
        verifyNoInteractions(scheduledMessagePort)
        verifyNoInteractions(scheduledMessageMapper)
    }

    @Test
    @DisplayName("채팅방에 속하지 않은 사용자가 메시지를 예약하면 예외가 발생한다")
    fun `채팅방에 속하지 않은 사용자가 메시지를 예약하면 예외가 발생한다`() {
        // given
        val roomId = 1L
        val senderId = 2L
        val content = "테스트 예약 메시지"
        val scheduledAt = Instant.now().plus(1, ChronoUnit.HOURS)

        val chatRoom = ChatRoom(
            id = roomId,
            title = "테스트 채팅방",
            type = ChatRoomType.GROUP,
            participants = mutableSetOf(3L, 4L) // senderId는 포함되지 않음
        )

        doReturn(chatRoom).`when`(loadChatRoomPort).findById(roomId)

        // when & then
        val exception = assertThrows<ApiException> {
            sut.scheduleMessage(roomId, senderId, content, scheduledAt)
        }

        assertThat(exception.errorCode).isEqualTo(ErrorCode.USER_NOT_IN_ROOM)
        verify(loadChatRoomPort).findById(roomId)
        verifyNoInteractions(scheduledMessagePort)
        verifyNoInteractions(scheduledMessageMapper)
    }

    @Test
    @DisplayName("과거 시간으로 메시지를 예약하면 예외가 발생한다")
    fun `과거 시간으로 메시지를 예약하면 예외가 발생한다`() {
        // given
        val roomId = 1L
        val senderId = 2L
        val content = "테스트 예약 메시지"
        val scheduledAt = Instant.now().minus(1, ChronoUnit.HOURS) // 과거 시간

        val chatRoom = ChatRoom(
            id = roomId,
            title = "테스트 채팅방",
            type = ChatRoomType.GROUP,
            participants = mutableSetOf(senderId, 3L, 4L)
        )

        doReturn(chatRoom).`when`(loadChatRoomPort).findById(roomId)

        // when & then
        val exception = assertThrows<ApiException> {
            sut.scheduleMessage(roomId, senderId, content, scheduledAt)
        }

        assertThat(exception.errorCode).isEqualTo(ErrorCode.INVALID_SCHEDULED_TIME)
        verify(loadChatRoomPort).findById(roomId)
        verifyNoInteractions(scheduledMessagePort)
        verifyNoInteractions(scheduledMessageMapper)
    }

}
