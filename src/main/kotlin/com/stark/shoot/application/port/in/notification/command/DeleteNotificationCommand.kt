package com.stark.shoot.application.port.`in`.notification.command

import com.stark.shoot.domain.notification.vo.NotificationId
import com.stark.shoot.domain.shared.UserId

/**
 * 알림 삭제 커맨드
 *
 * @property notificationId 삭제할 알림 ID
 * @property userId 사용자 ID
 */
data class DeleteNotificationCommand(
    val notificationId: NotificationId,
    val userId: UserId
)