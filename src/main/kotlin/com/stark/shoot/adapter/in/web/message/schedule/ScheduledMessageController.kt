package com.stark.shoot.adapter.`in`.web.message.schedule

import com.stark.shoot.adapter.`in`.web.dto.ResponseDto
import com.stark.shoot.adapter.`in`.web.dto.message.schedule.ScheduledMessageResponseDto
import com.stark.shoot.application.port.`in`.message.schedule.ScheduledMessageUseCase
import com.stark.shoot.domain.chat.room.vo.ChatRoomId
import com.stark.shoot.domain.common.vo.UserId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import java.time.LocalDateTime
import java.time.ZoneId

@Tag(name = "메시지 예약", description = "메시지 예약 관련 API")
@RestController
@RequestMapping("/api/v1/messages/scheduled")
class ScheduledMessageController(
    private val scheduledMessageUseCase: ScheduledMessageUseCase
) {

    @Operation(summary = "메시지 예약", description = "지정된 시간에 전송될 메시지를 예약합니다.")
    @PostMapping
    fun scheduleMessage(
        @RequestParam roomId: Long,
        @RequestParam content: String,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) scheduledAt: LocalDateTime,
        authentication: Authentication
    ): ResponseDto<ScheduledMessageResponseDto> {
        val userId = authentication.name.toLong()
        val scheduledInstant = scheduledAt.atZone(ZoneId.systemDefault()).toInstant()

        val result = scheduledMessageUseCase.scheduleMessage(
            roomId = ChatRoomId.from(roomId),
            senderId = UserId.from(userId),
            content = content,
            scheduledAt = scheduledInstant
        )

        return ResponseDto.success(result, "메시지가 예약되었습니다.")
    }

    @Operation(summary = "메시지 예약 취소", description = "예약된 메시지를 취소합니다.")
    @DeleteMapping("/{scheduledMessageId}")
    fun cancelScheduledMessage(
        @PathVariable scheduledMessageId: String,
        authentication: Authentication
    ): ResponseDto<ScheduledMessageResponseDto> {
        val userId = authentication.name.toLong()

        val result = scheduledMessageUseCase.cancelScheduledMessage(
            scheduledMessageId = scheduledMessageId,
            userId = UserId.from(userId)
        )

        return ResponseDto.success(result, "메시지 예약이 취소되었습니다.")
    }

    @Operation(summary = "메시지 예약 수정", description = "예약된 메시지의 내용 또는 예약 시간을 수정합니다.")
    @PutMapping("/{scheduledMessageId}")
    fun updateScheduledMessage(
        @PathVariable scheduledMessageId: String,
        @RequestParam newContent: String,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) newScheduledAt: LocalDateTime?,
        authentication: Authentication
    ): ResponseDto<ScheduledMessageResponseDto> {
        val userId = authentication.name.toLong()
        val newScheduledInstant = newScheduledAt?.atZone(ZoneId.systemDefault())?.toInstant()

        val result = scheduledMessageUseCase.updateScheduledMessage(
            scheduledMessageId = scheduledMessageId,
            userId = userId,
            newContent = newContent,
            newScheduledAt = newScheduledInstant
        )

        return ResponseDto.success(result, "메시지 예약이 수정되었습니다.")
    }

    @Operation(summary = "예약된 메시지 목록 조회", description = "사용자가 예약한 메시지 목록을 조회합니다.")
    @GetMapping
    fun getScheduledMessages(
        @RequestParam(required = false) roomId: Long?,
        authentication: Authentication
    ): ResponseDto<List<ScheduledMessageResponseDto>> {
        val userId = authentication.name.toLong()

        val result = scheduledMessageUseCase.getScheduledMessagesByUser(
            userId = userId,
            roomId = roomId
        )

        return ResponseDto.success(result)
    }

    @Operation(summary = "예약된 메시지 즉시 전송", description = "예약된 메시지를 즉시 전송합니다.")
    @PostMapping("/{scheduledMessageId}/send-now")
    fun sendScheduledMessageNow(
        @PathVariable scheduledMessageId: String,
        authentication: Authentication
    ): ResponseDto<ScheduledMessageResponseDto> {
        val userId = authentication.name.toLong()

        val result = scheduledMessageUseCase.sendScheduledMessageNow(
            scheduledMessageId = scheduledMessageId,
            userId = userId
        )

        return ResponseDto.success(result, "메시지가 즉시 전송되었습니다.")
    }

}