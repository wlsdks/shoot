package com.stark.shoot.adapter.`in`.web.notification

import com.stark.shoot.adapter.`in`.web.dto.notification.NotificationResponse
import com.stark.shoot.application.port.`in`.notification.NotificationQueryUseCase
import com.stark.shoot.domain.common.vo.UserId
import com.stark.shoot.domain.notification.type.NotificationType
import com.stark.shoot.domain.notification.type.SourceType
import io.swagger.v3.oas.annotations.Operation
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/notifications")
class NotificationQueryController(
    private val notificationQueryUseCase: NotificationQueryUseCase
) {

    @Operation(
        summary = "알림 조회",
        description = """
            - 사용자의 알림을 조회합니다.
            - 기본적으로 최신 20개의 알림을 조회합니다.
            - limit과 offset을 사용하여 페이지네이션을 지원합니다.
        """
    )
    @GetMapping
    fun getNotifications(
        @RequestParam userId: Long,
        @RequestParam(defaultValue = "20") limit: Int,
        @RequestParam(defaultValue = "0") offset: Int
    ): ResponseEntity<List<NotificationResponse>> {
        val notifications = notificationQueryUseCase
            .getNotificationsForUser(UserId.from(userId), limit, offset)
        return ResponseEntity.ok(NotificationResponse.from(notifications))
    }


    @Operation(
        summary = "읽지 않은 알림 조회",
        description = """
            - 사용자의 읽지 않은 알림을 조회합니다.
            - 기본적으로 최신 20개의 읽지 않은 알림을 조회합니다.
            - limit과 offset을 사용하여 페이지네이션을 지원합니다.
        """
    )
    @GetMapping("/unread")
    fun getUnreadNotifications(
        @RequestParam userId: Long,
        @RequestParam(defaultValue = "20") limit: Int,
        @RequestParam(defaultValue = "0") offset: Int
    ): ResponseEntity<List<NotificationResponse>> {
        val notifications = notificationQueryUseCase
            .getUnreadNotificationsForUser(UserId.from(userId), limit, offset)
        return ResponseEntity.ok(NotificationResponse.from(notifications))
    }


    @Operation(
        summary = "읽지 않은 알림 개수 조회",
        description = """
            - 사용자의 읽지 않은 알림 개수를 조회합니다.
        """
    )
    @GetMapping("/unread/count")
    fun getUnreadNotificationCount(@RequestParam userId: Long): ResponseEntity<Int> {
        val count = notificationQueryUseCase.getUnreadNotificationCount(UserId.from(userId))
        return ResponseEntity.ok(count)
    }


    @Operation(
        summary = "알림 타입별 조회",
        description = """
            - 사용자의 알림을 타입별로 조회합니다.
            - 기본적으로 최신 20개의 알림을 조회합니다.
            - limit과 offset을 사용하여 페이지네이션을 지원합니다.
        """
    )
    @GetMapping("/type/{type}")
    fun getNotificationsByType(
        @RequestParam userId: Long,
        @PathVariable type: String,
        @RequestParam(defaultValue = "20") limit: Int,
        @RequestParam(defaultValue = "0") offset: Int
    ): ResponseEntity<List<NotificationResponse>> {
        val notificationType = NotificationType.valueOf(type)
        val notifications = notificationQueryUseCase
            .getNotificationsByType(UserId.from(userId), notificationType, limit, offset)
        return ResponseEntity.ok(NotificationResponse.from(notifications))
    }


    @Operation(
        summary = "알림 소스별 조회",
        description = """
            - 사용자의 알림을 소스별로 조회합니다.
            - 기본적으로 최신 20개의 알림을 조회합니다.
            - limit과 offset을 사용하여 페이지네이션을 지원합니다.
        """
    )
    @GetMapping("/source/{sourceType}")
    fun getNotificationsBySource(
        @RequestParam userId: Long,
        @PathVariable sourceType: String,
        @RequestParam(required = false) sourceId: String?,
        @RequestParam(defaultValue = "20") limit: Int,
        @RequestParam(defaultValue = "0") offset: Int
    ): ResponseEntity<List<NotificationResponse>> {
        val source = SourceType.valueOf(sourceType)
        val notifications = notificationQueryUseCase
            .getNotificationsBySource(UserId.from(userId), source, sourceId, limit, offset)
        return ResponseEntity.ok(NotificationResponse.from(notifications))
    }

}