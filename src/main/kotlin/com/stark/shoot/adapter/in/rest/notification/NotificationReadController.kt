package com.stark.shoot.adapter.`in`.rest.notification

import com.stark.shoot.adapter.`in`.rest.dto.notification.NotificationResponse
import com.stark.shoot.application.port.`in`.notification.NotificationManagementUseCase
import com.stark.shoot.application.port.`in`.notification.command.MarkAllNotificationsAsReadCommand
import com.stark.shoot.application.port.`in`.notification.command.MarkAllNotificationsBySourceAsReadCommand
import com.stark.shoot.application.port.`in`.notification.command.MarkAllNotificationsByTypeAsReadCommand
import com.stark.shoot.application.port.`in`.notification.command.MarkNotificationAsReadCommand
import com.stark.shoot.domain.notification.type.NotificationType
import com.stark.shoot.domain.notification.type.SourceType
import com.stark.shoot.domain.notification.vo.NotificationId
import com.stark.shoot.domain.user.vo.UserId
import io.swagger.v3.oas.annotations.Operation
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/notifications")
class NotificationReadController(
    private val notificationManagementUseCase: NotificationManagementUseCase
) {

    @Operation(
        summary = "알림 읽음 처리",
        description = """
            - 특정 알림을 읽음 처리합니다.
            - 알림 ID와 사용자 ID를 사용하여 알림을 읽음 처리합니다.
        """
    )
    @PutMapping("/{notificationId}/read")
    fun markAsRead(
        @PathVariable notificationId: String,
        @RequestParam userId: Long
    ): ResponseEntity<NotificationResponse> {
        val command = MarkNotificationAsReadCommand(
            notificationId = NotificationId.from(notificationId),
            userId = UserId.from(userId)
        )
        val notification = notificationManagementUseCase.markAsRead(command)

        return ResponseEntity.ok(NotificationResponse.from(notification))
    }


    @Operation(
        summary = "모든 알림 읽음 처리",
        description = """
            - 사용자의 모든 알림을 읽음 처리합니다.
            - 사용자 ID를 사용하여 모든 알림을 읽음 처리합니다.
        """
    )
    @PutMapping("/read/all")
    fun markAllAsRead(@RequestParam userId: Long): ResponseEntity<Int> {
        val command = MarkAllNotificationsAsReadCommand(
            userId = UserId.from(userId)
        )
        val count = notificationManagementUseCase.markAllAsRead(command)
        return ResponseEntity.ok(count)
    }


    @Operation(
        summary = "모든 알림 타입별 읽음 처리",
        description = """
            - 사용자의 모든 알림을 타입별로 읽음 처리합니다.
            - 사용자 ID와 알림 타입을 사용하여 모든 알림을 읽음 처리합니다.
        """
    )
    @PutMapping("/read/type/{type}")
    fun markAllAsReadByType(
        @RequestParam userId: Long,
        @PathVariable type: String
    ): ResponseEntity<Int> {
        val notificationType = NotificationType.valueOf(type)
        val command = MarkAllNotificationsByTypeAsReadCommand(
            userId = UserId.from(userId),
            type = notificationType
        )
        val count = notificationManagementUseCase.markAllAsReadByType(command)
        return ResponseEntity.ok(count)
    }


    @Operation(
        summary = "모든 알림 소스별 읽음 처리",
        description = """
            - 사용자의 모든 알림을 소스별로 읽음 처리합니다.
            - 사용자 ID와 소스 타입을 사용하여 모든 알림을 읽음 처리합니다.
        """
    )
    @PutMapping("/read/source/{sourceType}")
    fun markAllAsReadBySource(
        @RequestParam userId: Long,
        @PathVariable sourceType: String,
        @RequestParam(required = false) sourceId: String?
    ): ResponseEntity<Int> {
        val source = SourceType.valueOf(sourceType)
        val command = MarkAllNotificationsBySourceAsReadCommand(
            userId = UserId.from(userId),
            sourceType = source,
            sourceId = sourceId
        )
        val count = notificationManagementUseCase.markAllAsReadBySource(command)
        return ResponseEntity.ok(count)
    }

}
