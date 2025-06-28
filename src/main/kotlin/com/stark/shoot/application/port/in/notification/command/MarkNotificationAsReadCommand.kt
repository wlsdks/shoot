package com.stark.shoot.application.port.`in`.notification.command

import com.stark.shoot.domain.notification.vo.NotificationId
import com.stark.shoot.domain.user.vo.UserId

/**
 * 알림을 읽음 상태로 표시하는 커맨드
 *
 * @property notificationId 읽음 처리할 알림 ID
 * @property userId 사용자 ID
 */
data class MarkNotificationAsReadCommand(
    val notificationId: NotificationId,
    val userId: UserId
)