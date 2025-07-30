package com.stark.shoot.adapter.`in`.rest.notification

import com.stark.shoot.adapter.`in`.rest.dto.ResponseDto
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
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*

@Tag(name = "알림", description = "알림 관련 API")
@RestController
@RequestMapping("/api/v1/notifications")
class NotificationReadController(
    private val notificationManagementUseCase: NotificationManagementUseCase
) {

    /**
     * Marks a specific notification as read for the authenticated user.
     *
     * @param notificationId The ID of the notification to mark as read.
     * @return A response containing the updated notification data and a success message.
     */
    @Operation(
        summary = "알림 읽음 처리",
        description = """
            - 특정 알림을 읽음 처리합니다.
            - 알림 ID를 사용하여 알림을 읽음 처리합니다.
        """
    )
    @PutMapping("/{notificationId}/read")
    fun markAsRead(
        authentication: Authentication,
        @PathVariable notificationId: String
    ): ResponseDto<NotificationResponse> {
        val userId = authentication.name.toLong()
        val command = MarkNotificationAsReadCommand(
            notificationId = NotificationId.from(notificationId),
            userId = UserId.from(userId)
        )
        val notification = notificationManagementUseCase.markAsRead(command)

        return ResponseDto.success(NotificationResponse.from(notification), "알림이 읽음 처리되었습니다.")
    }


    /**
     * Marks all notifications as read for the authenticated user.
     *
     * @return A response containing the number of notifications marked as read.
     */
    @Operation(
        summary = "모든 알림 읽음 처리",
        description = """
            - 사용자의 모든 알림을 읽음 처리합니다.
        """
    )
    @PutMapping("/read/all")
    fun markAllAsRead(authentication: Authentication): ResponseDto<Int> {
        val userId = authentication.name.toLong()
        val command = MarkAllNotificationsAsReadCommand(
            userId = UserId.from(userId)
        )
        val count = notificationManagementUseCase.markAllAsRead(command)
        return ResponseDto.success(count, "모든 알림이 읽음 처리되었습니다.")
    }


    /**
     * Marks all notifications of a specified type as read for the authenticated user.
     *
     * @param type The notification type to mark as read.
     * @return The number of notifications marked as read.
     */
    @Operation(
        summary = "모든 알림 타입별 읽음 처리",
        description = """
            - 사용자의 모든 알림을 타입별로 읽음 처리합니다.
            - 알림 타입을 사용하여 모든 알림을 읽음 처리합니다.
        """
    )
    @PutMapping("/read/type/{type}")
    fun markAllAsReadByType(
        authentication: Authentication,
        @PathVariable type: String
    ): ResponseDto<Int> {
        val userId = authentication.name.toLong()
        val notificationType = NotificationType.valueOf(type)
        val command = MarkAllNotificationsByTypeAsReadCommand(
            userId = UserId.from(userId),
            type = notificationType
        )
        val count = notificationManagementUseCase.markAllAsReadByType(command)
        return ResponseDto.success(count, "타입별 알림이 읽음 처리되었습니다.")
    }


    /**
     * Marks all notifications from a specific source type, and optionally a specific source ID, as read for the authenticated user.
     *
     * @param sourceType The type of the notification source to filter by.
     * @param sourceId Optional ID of the specific source to further filter notifications.
     * @return The number of notifications marked as read.
     */
    @Operation(
        summary = "모든 알림 소스별 읽음 처리",
        description = """
            - 사용자의 모든 알림을 소스별로 읽음 처리합니다.
            - 소스 타입을 사용하여 모든 알림을 읽음 처리합니다.
        """
    )
    @PutMapping("/read/source/{sourceType}")
    fun markAllAsReadBySource(
        authentication: Authentication,
        @PathVariable sourceType: String,
        @RequestParam(required = false) sourceId: String?
    ): ResponseDto<Int> {
        val userId = authentication.name.toLong()
        val source = SourceType.valueOf(sourceType)
        val command = MarkAllNotificationsBySourceAsReadCommand(
            userId = UserId.from(userId),
            sourceType = source,
            sourceId = sourceId
        )
        val count = notificationManagementUseCase.markAllAsReadBySource(command)
        return ResponseDto.success(count, "소스별 알림이 읽음 처리되었습니다.")
    }

}