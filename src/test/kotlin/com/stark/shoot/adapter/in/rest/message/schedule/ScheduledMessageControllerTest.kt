package com.stark.shoot.adapter.`in`.rest.message.schedule

import com.stark.shoot.adapter.`in`.rest.dto.message.MessageContentResponseDto
import com.stark.shoot.adapter.`in`.rest.dto.message.MessageMetadataResponseDto
import com.stark.shoot.adapter.`in`.rest.dto.message.schedule.ScheduledMessageResponseDto
import com.stark.shoot.adapter.`in`.rest.dto.message.schedule.ScheduleMessageRequest
import com.stark.shoot.adapter.`in`.rest.dto.message.schedule.ScheduleMessageSendNowRequest
import com.stark.shoot.application.port.`in`.message.schedule.ScheduledMessageUseCase
import com.stark.shoot.application.port.`in`.message.schedule.command.*
import com.stark.shoot.domain.chat.message.type.MessageType
import com.stark.shoot.domain.chat.message.type.ScheduledMessageStatus
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import org.springframework.security.core.Authentication
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import org.hamcrest.Matchers.hasSize

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

        val command = ScheduleMessageCommand.of(roomId, userId, content, scheduledInstant)
        `when`(scheduledMessageUseCase.scheduleMessage(command)).thenReturn(responseDto)

        // when
        val request = ScheduleMessageRequest(roomId, content, scheduledAt)
        val response = controller.scheduleMessage(request, authentication)

        // then
        assertThat(response).isNotNull
        assertThat(response.success).isTrue()
        assertThat(response.data).isEqualTo(responseDto)
        assertThat(response.message).isEqualTo("메시지가 예약되었습니다.")
        
        verify(scheduledMessageUseCase).scheduleMessage(command)
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

        val command = CancelScheduledMessageCommand.of(scheduledMessageId, userId)
        `when`(scheduledMessageUseCase.cancelScheduledMessage(command)).thenReturn(responseDto)

        // when
        val response = controller.cancelScheduledMessage(scheduledMessageId, authentication)

        // then
        assertThat(response).isNotNull
        assertThat(response.success).isTrue()
        assertThat(response.data).isEqualTo(responseDto)
        assertThat(response.message).isEqualTo("메시지 예약이 취소되었습니다.")
        
        verify(scheduledMessageUseCase).cancelScheduledMessage(command)
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

        val command = UpdateScheduledMessageCommand.of(scheduledMessageId, userId, newContent, newScheduledInstant)
        `when`(scheduledMessageUseCase.updateScheduledMessage(command)).thenReturn(responseDto)

        // when
        val response = controller.updateScheduledMessage(scheduledMessageId, newContent, newScheduledAt, authentication)

        // then
        assertThat(response).isNotNull
        assertThat(response.success).isTrue()
        assertThat(response.data).isEqualTo(responseDto)
        assertThat(response.message).isEqualTo("메시지 예약이 수정되었습니다.")
        
        verify(scheduledMessageUseCase).updateScheduledMessage(command)
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

        val command = GetScheduledMessagesCommand.of(userId, roomId)
        `when`(scheduledMessageUseCase.getScheduledMessagesByUser(command)).thenReturn(responseDtos)

        // when
        val response = controller.getScheduledMessages(roomId, authentication)

        // then
        assertThat(response).isNotNull
        assertThat(response.success).isTrue()
        assertThat(response.data).hasSize(2)
        assertThat(response.data).isEqualTo(responseDtos)
        
        verify(scheduledMessageUseCase).getScheduledMessagesByUser(command)
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

        val command = SendScheduledMessageNowCommand.of(scheduledMessageId, userId)
        `when`(scheduledMessageUseCase.sendScheduledMessageNow(command)).thenReturn(responseDto)

        // when
        val request = ScheduleMessageSendNowRequest(scheduledMessageId)
        val response = controller.sendScheduledMessageNow(request, authentication)

        // then
        assertThat(response).isNotNull
        assertThat(response.success).isTrue()
        assertThat(response.data).isEqualTo(responseDto)
        assertThat(response.message).isEqualTo("메시지가 즉시 전송되었습니다.")
        
        verify(scheduledMessageUseCase).sendScheduledMessageNow(command)
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