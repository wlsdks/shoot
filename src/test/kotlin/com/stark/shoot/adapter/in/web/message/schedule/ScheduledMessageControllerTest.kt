package com.stark.shoot.adapter.`in`.web.message.schedule

import com.stark.shoot.adapter.`in`.web.dto.message.MessageContentResponseDto
import com.stark.shoot.adapter.`in`.web.dto.message.MessageMetadataResponseDto
import com.stark.shoot.adapter.`in`.web.dto.message.schedule.ScheduledMessageResponseDto
import com.stark.shoot.application.port.`in`.message.schedule.ScheduledMessageUseCase
import com.stark.shoot.domain.chat.message.type.MessageType
import com.stark.shoot.domain.chat.message.type.ScheduledMessageStatus
import com.stark.shoot.domain.chatroom.vo.ChatRoomId
import com.stark.shoot.domain.user.vo.UserId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import org.springframework.security.core.Authentication
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

@DisplayName("ScheduledMessageController 단위 테스트")
class ScheduledMessageControllerTest {

    private val scheduledMessageUseCase = mock(ScheduledMessageUseCase::class.java)
    private val authentication = mock(Authentication::class.java)
    private val controller = ScheduledMessageController(scheduledMessageUseCase)

    @Test
    @DisplayName("[happy] 메시지 예약 요청을 처리하고 예약된 메시지를 반환한다")
    fun `메시지 예약 요청을 처리하고 예약된 메시지를 반환한다`() {
        // given
        val roomId = 1L
        val userId = 2L
        val content = "예약된 메시지 내용"
        val scheduledAt = LocalDateTime.now().plusHours(1)
        val scheduledInstant = scheduledAt.atZone(ZoneId.systemDefault()).toInstant()

        `when`(authentication.name).thenReturn(userId.toString())

        val responseDto = createScheduledMessageResponseDto(
            id = "scheduled123",
            roomId = roomId,
            senderId = userId,
            content = content,
            scheduledAt = scheduledInstant
        )

        `when`(scheduledMessageUseCase.scheduleMessage(
            roomId = ChatRoomId.from(roomId),
            senderId = UserId.from(userId),
            content = content,
            scheduledAt = scheduledInstant
        )).thenReturn(responseDto)

        // when
        val response = controller.scheduleMessage(roomId, content, scheduledAt, authentication)

        // then
        assertThat(response).isNotNull
        assertThat(response.success).isTrue()
        assertThat(response.data).isEqualTo(responseDto)
        assertThat(response.message).isEqualTo("메시지가 예약되었습니다.")

        verify(scheduledMessageUseCase).scheduleMessage(
            roomId = ChatRoomId.from(roomId),
            senderId = UserId.from(userId),
            content = content,
            scheduledAt = scheduledInstant
        )
    }

    @Test
    @DisplayName("[happy] 메시지 예약 취소 요청을 처리하고 취소된 메시지를 반환한다")
    fun `메시지 예약 취소 요청을 처리하고 취소된 메시지를 반환한다`() {
        // given
        val scheduledMessageId = "scheduled123"
        val userId = 2L

        `when`(authentication.name).thenReturn(userId.toString())

        val responseDto = createScheduledMessageResponseDto(
            id = scheduledMessageId,
            roomId = 1L,
            senderId = userId,
            content = "취소된 예약 메시지",
            scheduledAt = Instant.now().plusSeconds(3600),
            status = ScheduledMessageStatus.CANCELED
        )

        `when`(scheduledMessageUseCase.cancelScheduledMessage(
            scheduledMessageId = scheduledMessageId,
            userId = UserId.from(userId)
        )).thenReturn(responseDto)

        // when
        val response = controller.cancelScheduledMessage(scheduledMessageId, authentication)

        // then
        assertThat(response).isNotNull
        assertThat(response.success).isTrue()
        assertThat(response.data).isEqualTo(responseDto)
        assertThat(response.message).isEqualTo("메시지 예약이 취소되었습니다.")

        verify(scheduledMessageUseCase).cancelScheduledMessage(
            scheduledMessageId = scheduledMessageId,
            userId = UserId.from(userId)
        )
    }

    @Test
    @DisplayName("[happy] 메시지 예약 수정 요청을 처리하고 수정된 메시지를 반환한다")
    fun `메시지 예약 수정 요청을 처리하고 수정된 메시지를 반환한다`() {
        // given
        val scheduledMessageId = "scheduled123"
        val userId = 2L
        val newContent = "수정된 예약 메시지 내용"
        val newScheduledAt = LocalDateTime.now().plusHours(2)
        val newScheduledInstant = newScheduledAt.atZone(ZoneId.systemDefault()).toInstant()

        `when`(authentication.name).thenReturn(userId.toString())

        val responseDto = createScheduledMessageResponseDto(
            id = scheduledMessageId,
            roomId = 1L,
            senderId = userId,
            content = newContent,
            scheduledAt = newScheduledInstant
        )

        `when`(scheduledMessageUseCase.updateScheduledMessage(
            scheduledMessageId = scheduledMessageId,
            userId = userId,
            newContent = newContent,
            newScheduledAt = newScheduledInstant
        )).thenReturn(responseDto)

        // when
        val response = controller.updateScheduledMessage(scheduledMessageId, newContent, newScheduledAt, authentication)

        // then
        assertThat(response).isNotNull
        assertThat(response.success).isTrue()
        assertThat(response.data).isEqualTo(responseDto)
        assertThat(response.message).isEqualTo("메시지 예약이 수정되었습니다.")

        verify(scheduledMessageUseCase).updateScheduledMessage(
            scheduledMessageId = scheduledMessageId,
            userId = userId,
            newContent = newContent,
            newScheduledAt = newScheduledInstant
        )
    }

    @Test
    @DisplayName("[happy] 예약된 메시지 목록을 조회한다")
    fun `예약된 메시지 목록을 조회한다`() {
        // given
        val userId = 2L
        val roomId = 1L

        `when`(authentication.name).thenReturn(userId.toString())

        val responseDtos = listOf(
            createScheduledMessageResponseDto(
                id = "scheduled123",
                roomId = roomId,
                senderId = userId,
                content = "첫 번째 예약 메시지",
                scheduledAt = Instant.now().plusSeconds(3600)
            ),
            createScheduledMessageResponseDto(
                id = "scheduled456",
                roomId = roomId,
                senderId = userId,
                content = "두 번째 예약 메시지",
                scheduledAt = Instant.now().plusSeconds(7200)
            )
        )

        `when`(scheduledMessageUseCase.getScheduledMessagesByUser(
            userId = userId,
            roomId = roomId
        )).thenReturn(responseDtos)

        // when
        val response = controller.getScheduledMessages(roomId, authentication)

        // then
        assertThat(response).isNotNull
        assertThat(response.success).isTrue()
        assertThat(response.data).hasSize(2)
        assertThat(response.data).isEqualTo(responseDtos)

        verify(scheduledMessageUseCase).getScheduledMessagesByUser(
            userId = userId,
            roomId = roomId
        )
    }

    @Test
    @DisplayName("[happy] 예약된 메시지를 즉시 전송한다")
    fun `예약된 메시지를 즉시 전송한다`() {
        // given
        val scheduledMessageId = "scheduled123"
        val userId = 2L

        `when`(authentication.name).thenReturn(userId.toString())

        val responseDto = createScheduledMessageResponseDto(
            id = scheduledMessageId,
            roomId = 1L,
            senderId = userId,
            content = "즉시 전송된 예약 메시지",
            scheduledAt = Instant.now(),
            status = ScheduledMessageStatus.SENT
        )

        `when`(scheduledMessageUseCase.sendScheduledMessageNow(
            scheduledMessageId = scheduledMessageId,
            userId = userId
        )).thenReturn(responseDto)

        // when
        val response = controller.sendScheduledMessageNow(scheduledMessageId, authentication)

        // then
        assertThat(response).isNotNull
        assertThat(response.success).isTrue()
        assertThat(response.data).isEqualTo(responseDto)
        assertThat(response.message).isEqualTo("메시지가 즉시 전송되었습니다.")

        verify(scheduledMessageUseCase).sendScheduledMessageNow(
            scheduledMessageId = scheduledMessageId,
            userId = userId
        )
    }

    // 테스트용 ScheduledMessageResponseDto 객체 생성 헬퍼 메서드
    private fun createScheduledMessageResponseDto(
        id: String,
        roomId: Long,
        senderId: Long,
        content: String,
        scheduledAt: Instant,
        status: ScheduledMessageStatus = ScheduledMessageStatus.PENDING
    ): ScheduledMessageResponseDto {
        val now = Instant.now()
        return ScheduledMessageResponseDto(
            id = id,
            roomId = roomId,
            senderId = senderId,
            content = MessageContentResponseDto(
                text = content,
                type = MessageType.TEXT,
                isEdited = false,
                isDeleted = false
            ),
            scheduledAt = scheduledAt,
            createdAt = now,
            status = status,
            metadata = MessageMetadataResponseDto()
        )
    }
}
