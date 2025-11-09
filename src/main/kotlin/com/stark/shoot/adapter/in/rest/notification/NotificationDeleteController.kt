package com.stark.shoot.adapter.`in`.rest.notification

import com.stark.shoot.adapter.`in`.rest.dto.ResponseDto
import com.stark.shoot.application.port.`in`.notification.NotificationManagementUseCase
import com.stark.shoot.application.port.`in`.notification.command.DeleteAllNotificationsCommand
import com.stark.shoot.application.port.`in`.notification.command.DeleteNotificationCommand
import com.stark.shoot.domain.notification.vo.NotificationId
import com.stark.shoot.domain.shared.UserId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*

@Tag(name = "알림", description = "알림 관련 API")
@RestController
@RequestMapping("/api/v1/notifications")
class NotificationDeleteController(
    private val notificationManagementUseCase: NotificationManagementUseCase
) {

    @Operation(
        summary = "알림 삭제",
        description = """
            - 특정 알림을 삭제합니다.
            - 알림 ID와 사용자 ID를 사용하여 알림을 삭제합니다.
        """
    )
    @DeleteMapping("/{notificationId}")
    fun deleteNotification(
        authentication: Authentication,
        @PathVariable notificationId: String,
    ): ResponseDto<Boolean> {
        val userId = authentication.name.toLong()
        val command = DeleteNotificationCommand(NotificationId.from(notificationId), UserId.from(userId))
        val deleted = notificationManagementUseCase.deleteNotification(command)
        return ResponseDto.success(deleted, "알림이 성공적으로 삭제되었습니다.")
    }


    @Operation(
        summary = "모든 알림 삭제",
        description = """
            - 사용자의 모든 알림을 삭제합니다.
            - 사용자 ID를 사용하여 모든 알림을 삭제합니다.
        """
    )
    @DeleteMapping
    fun deleteAllNotifications(
        authentication: Authentication
    ): ResponseDto<Int> {
        val userId = authentication.name.toLong()
        val command = DeleteAllNotificationsCommand(UserId.from(userId))
        val count = notificationManagementUseCase.deleteAllNotifications(command)
        return ResponseDto.success(count, "모든 알림이 성공적으로 삭제되었습니다.")
    }

}