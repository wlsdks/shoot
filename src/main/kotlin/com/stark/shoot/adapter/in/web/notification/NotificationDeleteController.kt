package com.stark.shoot.adapter.`in`.web.notification

import com.stark.shoot.application.port.`in`.notification.NotificationManagementUseCase
import com.stark.shoot.domain.common.vo.UserId
import com.stark.shoot.domain.notification.vo.NotificationId
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
        val deleted = notificationManagementUseCase.deleteNotification(
            NotificationId.from(notificationId),
            UserId.from(userId)
        )

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
        val count = notificationManagementUseCase.deleteAllNotifications(UserId.from(userId))
        return ResponseEntity.ok(count)
    }

}