package com.stark.shoot.application.port.`in`.notification.command

import com.stark.shoot.domain.notification.type.NotificationType
import com.stark.shoot.domain.shared.UserId

/**
 * 특정 타입의 모든 알림을 읽음 상태로 표시하는 커맨드
 *
 * @property userId 사용자 ID
 * @property type 알림 타입
 */
data class MarkAllNotificationsByTypeAsReadCommand(
    val userId: UserId,
    val type: NotificationType
)