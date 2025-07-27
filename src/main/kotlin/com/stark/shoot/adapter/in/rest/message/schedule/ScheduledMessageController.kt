package com.stark.shoot.adapter.`in`.rest.message.schedule

import com.stark.shoot.adapter.`in`.rest.dto.ResponseDto
import com.stark.shoot.adapter.`in`.rest.dto.message.schedule.ScheduleMessageRequest
import com.stark.shoot.adapter.`in`.rest.dto.message.schedule.ScheduleMessageSendNowRequest
import com.stark.shoot.adapter.`in`.rest.dto.message.schedule.ScheduledMessageResponseDto
import com.stark.shoot.application.port.`in`.message.schedule.ScheduledMessageUseCase
import com.stark.shoot.application.port.`in`.message.schedule.command.*
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import java.time.LocalDateTime

@Tag(name = "메시지 예약", description = "메시지 예약 관련 API")
@RestController
@RequestMapping("/api/v1/messages/scheduled")
class ScheduledMessageController(
    private val scheduledMessageUseCase: ScheduledMessageUseCase
) {

    @Operation(
        summary = "메시지 전송 예약",
        description = "지정된 시간에 전송될 메시지를 예약합니다."
    )
    @PostMapping
    fun scheduleMessage(
        @RequestBody request: ScheduleMessageRequest,
        authentication: Authentication
    ): ResponseDto<ScheduledMessageResponseDto> {
        val command = ScheduleMessageCommand.of(request, authentication)
        val result = scheduledMessageUseCase.scheduleMessage(command)
        return ResponseDto.success(result, "메시지가 예약되었습니다.")
    }

    @Operation(
        summary = "메시지 전송 예약 취소",
        description = "예약된 메시지를 취소합니다."
    )
    @DeleteMapping("/{scheduledMessageId}")
    fun cancelScheduledMessage(
        @PathVariable scheduledMessageId: String,
        authentication: Authentication
    ): ResponseDto<ScheduledMessageResponseDto> {
        val command = CancelScheduledMessageCommand.of(scheduledMessageId, authentication)
        val result = scheduledMessageUseCase.cancelScheduledMessage(command)
        return ResponseDto.success(result, "메시지 예약이 취소되었습니다.")
    }

    @Operation(
        summary = "메시지 전송 예약 수정",
        description = "예약된 메시지의 내용 또는 예약 시간을 수정합니다."
    )
    @PutMapping("/{scheduledMessageId}")
    fun updateScheduledMessage(
        @PathVariable scheduledMessageId: String,
        @RequestParam newContent: String,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) newScheduledAt: LocalDateTime?,
        authentication: Authentication
    ): ResponseDto<ScheduledMessageResponseDto> {
        val command = UpdateScheduledMessageCommand.of(scheduledMessageId, authentication, newContent, newScheduledAt)
        val result = scheduledMessageUseCase.updateScheduledMessage(command)
        return ResponseDto.success(result, "메시지 예약이 수정되었습니다.")
    }

    @Operation(
        summary = "예약된 메시지 목록 조회",
        description = "사용자가 예약한 메시지 목록을 조회합니다."
    )
    @GetMapping
    fun getScheduledMessages(
        @RequestParam(required = false) roomId: Long?,
        authentication: Authentication
    ): ResponseDto<List<ScheduledMessageResponseDto>> {
        val command = GetScheduledMessagesCommand.of(authentication, roomId)
        val result = scheduledMessageUseCase.getScheduledMessagesByUser(command)
        return ResponseDto.success(result)
    }

    @Operation(
        summary = "예약된 메시지 즉시 전송",
        description = "예약된 메시지를 즉시 전송합니다."
    )
    @PostMapping("/{scheduledMessageId}/send-now")
    fun sendScheduledMessageNow(
        @RequestBody request: ScheduleMessageSendNowRequest,
        authentication: Authentication
    ): ResponseDto<ScheduledMessageResponseDto> {
        val command = SendScheduledMessageNowCommand.of(request, authentication)
        val result = scheduledMessageUseCase.sendScheduledMessageNow(command)
        return ResponseDto.success(result, "메시지가 즉시 전송되었습니다.")
    }

}
