package com.stark.shoot.application.port.`in`.notification.command

import com.stark.shoot.domain.user.vo.UserId

/**
 * 모든 알림을 읽음 상태로 표시하는 커맨드
 *
 * @property userId 사용자 ID
 */
data class MarkAllNotificationsAsReadCommand(
    val userId: UserId
)