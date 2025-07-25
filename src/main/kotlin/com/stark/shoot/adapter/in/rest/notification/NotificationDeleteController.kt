package com.stark.shoot.adapter.`in`.rest.notification

import com.stark.shoot.application.port.`in`.notification.NotificationManagementUseCase
import com.stark.shoot.application.port.`in`.notification.command.DeleteAllNotificationsCommand
import com.stark.shoot.application.port.`in`.notification.command.DeleteNotificationCommand
import com.stark.shoot.domain.notification.vo.NotificationId
import com.stark.shoot.domain.user.vo.UserId
import io.swagger.v3.oas.annotations.Operation
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/notifications")
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
        @PathVariable notificationId: String,
        @RequestParam userId: Long
    ): ResponseEntity<Boolean> {
        val command = DeleteNotificationCommand(
            notificationId = NotificationId.from(notificationId),
            userId = UserId.from(userId)
        )
        val deleted = notificationManagementUseCase.deleteNotification(command)

        return ResponseEntity.ok(deleted)
    }


    @Operation(
        summary = "모든 알림 삭제",
        description = """
            - 사용자의 모든 알림을 삭제합니다.
            - 사용자 ID를 사용하여 모든 알림을 삭제합니다.
        """
    )
    @DeleteMapping
    fun deleteAllNotifications(@RequestParam userId: Long): ResponseEntity<Int> {
        val command = DeleteAllNotificationsCommand(
            userId = UserId.from(userId)
        )
        val count = notificationManagementUseCase.deleteAllNotifications(command)
        return ResponseEntity.ok(count)
    }

}
